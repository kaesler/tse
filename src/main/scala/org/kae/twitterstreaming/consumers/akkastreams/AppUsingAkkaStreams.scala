package org.kae.twitterstreaming.consumers.akkastreams

import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{EntityStreamException, HttpRequest}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString

import de.knutwalker.akka.stream.JsonStreamParser
import io.circe.jawn.CirceSupportParser._

import org.kae.twitterstreaming.credentials.TwitterCredentialsProvider
import org.kae.twitterstreaming.statistics.CumulativeStatistics
import org.kae.twitterstreaming.streamcontents.{StallWarning, StreamElement, Tweet}

/**
 * Demo app to collect statistics from a Twitter stream using Akka HTTP and
 * Akka Streams.
 */
object AppUsingAkkaStreams
  extends App
  with RequestSigning
  with RequestBuilding {

  // Note: fail here early and hard if no credentials.
  private val creds = TwitterCredentialsProvider.required()

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()

  import system.dispatcher

  private val logger = Logging.getLogger(system, this)

  private val streamUrl =
    "https://stream.twitter.com/1.1/statuses/sample.json" +
      "?stall_warnings=true"

  private val TimeBetweenStatsReports = 15.seconds

  // TODO: Avoid this assignment and instead, on network failure make the
  // pipeline materialize the last stats computed so we can feed it back in
  // to a fresh pipeline.
  private val lastStats = new AtomicReference(CumulativeStatistics.empty)

  runFreshPipeline()
    // Note: terminate the ActorSystem and process if the response ends for
    // any other reason.
    .onComplete { _ ⇒ system.terminate() }

  private def runFreshPipeline(): Future[Done] = {
    consumeResponse(initiateResponse(), lastStats.get)
      .recoverWith {
        // Note:  recover from network interruptions by running a freshly
        // created pipeline.
        case ese: EntityStreamException
          if ese.getMessage === "Entity stream truncation" =>
          logger.warning("Recovering from response truncation")
          runFreshPipeline()
      }
  }

  private def initiateResponse(): Source[ByteString, NotUsed] =
    signAndSend(Get(streamUrl))

  private def signAndSend(req: HttpRequest): Source[ByteString, NotUsed] =
    Source.fromFutureSource(
      Http().singleRequest(sign(req, creds))
        .map(_.entity.dataBytes))
      .mapMaterializedValue(_ ⇒ NotUsed)

  private def consumeResponse(
      byteStrings: Source[ByteString, NotUsed],
      initialStats: CumulativeStatistics
  ): Future[Done] = {

    byteStrings

      // ByteString -> Json
      .via(JsonStreamParser.flow).async

      // Json -> StreamElement
      .map(StreamElement.apply).async

      // Log any stall warnings.
      .map {
        case StallWarning ⇒
          logger.warning("Stall warning occurred")
          StallWarning

        case other ⇒ other
      }

      // StreamElement -> Tweet
      .collect[Tweet] { case t: Tweet => t }

      // Tweet -> TweetDigest (in parallel)
      .mapAsyncUnordered(4) { tweet => Future.successful(tweet.digest) }

      // TweetDigest -> CumulativeStatistics
      .scan(initialStats) { (stats, digest) => stats.accumulate(digest) }.async

      // Keep the most recent CumulativeStatistics
      .conflate { (_, nextStats) =>
        // Save last stats computed in case the pipeline is restarted.
        lastStats.set(nextStats)

        nextStats
      }

      // Zip with a clock tick to emit at regular cadence.
      .zip(Source.tick(TimeBetweenStatsReports, TimeBetweenStatsReports, ()))

      // Run for a finite time.
      .takeWithin(30.minutes)

      // Ensure helpful error logging.
      .log("Tweet statistics pipeline")

      // Remove the tick -> CumulativeStatistics
      .map(_._1)

      // Hide initial empty stats.
      .filter { stats => stats.endTime !== stats.startTime }

      // Print report.
      .map(_.asText)
      .runForeach(println)
  }
}


