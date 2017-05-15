package org.kae.twitterstreaming.consumers.fs2

import java.time.Instant

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{EntityStreamException, HttpRequest}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.{Done, NotUsed}

import fs2.{Stream, Task}

import streamz.converter._

import org.kae.twitterstreaming.consumers.akkastreams.RequestSigning
import org.kae.twitterstreaming.credentials.TwitterCredentialsProvider
import org.kae.twitterstreaming.statistics.StatisticsAccumulator

/**
  * Demo app to collect statistics from a Twitter stream using Akka HTTP and
  * FS2 streams.
  */
object AppUsingFs2
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

  // Note: this is here outside the pipeline so that it will persist across
  // restarts of the pipeline due to network glitches.
  private val accumulator = new StatisticsAccumulator(Instant.now)

  runFreshPipeline()
    // Note: terminate the ActorSystem and process if the response ends for
    // any other reason.
    .onComplete { _ ⇒ system.terminate() }

  ////////////////////////////////////////////////////////////////////////

  private def runFreshPipeline(): Future[Done] = {
    consumeResponse(initiateResponse())
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
      byteStrings: Source[ByteString, NotUsed]
  ): Future[Done] = {

    // Get an FS2 Stream.
    val bss: Stream[Task, ByteString] = byteStrings.toStream()

    // Now see https://github.com/rossabaker/jawn-fs2

    ???
//      // ByteString -> Json
//      .via {
//      import CirceSupportParser._
//      JsonStreamParser.flow
//    }.async
//
//      // Json -> StreamElement
//      .map(StreamElement.apply).async
//      // Log any stall warnings.
//      .map {
//      case StallWarning ⇒
//        logger.warning("Stall warning occurred")
//        StallWarning
//      case other ⇒ other
//    }
//
//      // StreamElement -> Tweet
//      .collect[Tweet] { case t: Tweet => t }
//
//      // Tweet -> TweetDigest (in parallel)
//      .mapAsyncUnordered(4) { tweet =>
//      Future.successful(tweet.digest)
//    }
//
//      // TweetDigest -> StatisticsSnapshot
//      .via(new AccumulateStatistics(accumulator,TimeBetweenStatsReports)).async
//
//      // Run for a finite time.
//      .takeWithin(30.minutes)
//
//      // Ensure helpful error logging.
//      .log("Tweet statistics pipeline")
//
//      // Print stats snapshot
//      .map(_.asText)
//      .runForeach(println)
  }

}
