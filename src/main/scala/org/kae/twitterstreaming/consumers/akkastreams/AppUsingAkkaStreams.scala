package org.kae.twitterstreaming.consumers.akkastreams

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{EntityStreamException, HttpRequest}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString

import cats.implicits._
import de.knutwalker.akka.stream.JsonStreamParser
import io.circe.jawn.CirceSupportParser._

import org.kae.twitterstreaming.credentials.TwitterCredentialsProvider
import org.kae.twitterstreaming.statistics.Statistics
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

  runFreshPipeline(Statistics.Empty)
    .onComplete {
      case Success(lastStatsSeen) ⇒
        // Note: if pipeline stops, restart it with accumulated stats.
        logger.warning(s"Restarting pipeline after ${lastStatsSeen.tweetCount} tweets")
        runFreshPipeline(lastStatsSeen)

      case Failure(_: NoSuchElementException) ⇒
        logger.warning(s"Restarting pipeline before first tweet was seen")
        runFreshPipeline(Statistics.Empty)

      case Failure(t) ⇒
        logger.error(t, "")
        system.terminate()
    }

  private def runFreshPipeline(startingStats: Statistics): Future[Statistics] =
    consumeResponse(initiateResponse(), startingStats)

  private def initiateResponse(): Source[ByteString, NotUsed] = signAndSend(Get(streamUrl))

  private def signAndSend(req: HttpRequest): Source[ByteString, NotUsed] =
    Source.fromFutureSource(
      Http().singleRequest(sign(req, creds))
        .map(_.entity.dataBytes))
      .mapMaterializedValue(_ ⇒ NotUsed)

  private def consumeResponse(
    byteStrings: Source[ByteString, NotUsed],
    initialStats: Statistics
  ): Future[Statistics] = {

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
      //
      // Arrange for the stream to just end if a network error occurs.
      // Its completion will materialize the last-seen [[Statistics]] which can be used
      // as the initial value for a fresh pipeline.
      .collect[Try[Tweet]] { case t: Tweet => Success(t) }
      .recover {
        case ese: EntityStreamException
          if ese.getMessage === "Entity stream truncation" =>
            logger.warning("Stream truncated by error and will be restarted")
          Failure[Tweet](ese)
      }
      .takeWhile(_.isSuccess)
      .collect {
        case Success(tweet) ⇒ tweet
      }

      // Tweet -> Statistics (in parallel)
      // Note: Commutativity of [[Statistics#combine]] ensures that
      // the re-ordering here is permissible.
      .mapAsyncUnordered(4) { tweet =>
        Future { tweet.statistics }
      }

      // Statistics -> Statistics (total)
      .scan(initialStats) { (accumulatedStats, singleTweetStats) =>
        accumulatedStats combine singleTweetStats
      }.async

      // Keep the most recent Statistics until a tick is ready to zip it with.
      .conflate { (_, nextStats) =>  nextStats }

      // Zip with a clock tick to emit at regular cadence.
      .zip(Source.tick(TimeBetweenStatsReports, TimeBetweenStatsReports, ()))

      // Remove the tick -> Statistics
      .map[Statistics]{ case (stats, _ ) ⇒ stats }

      // Ensure helpful error logging.
      .log("Tweet statistics pipeline")

      // Hide initial empty stats.
      .filter(_.nonEmpty)

      // Print report.
      .map { stats ⇒ println(stats.asText); stats }

      // Materialize the last [[Statistics]] seen, if any.
      .toMat(Sink.last)(Keep.right)

      .run()
  }
}


