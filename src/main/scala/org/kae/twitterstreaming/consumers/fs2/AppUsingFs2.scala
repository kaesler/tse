package org.kae.twitterstreaming.consumers.fs2

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

import fs2.{Stream, Task}

import streamz.converter._

import org.kae.twitterstreaming.consumers.akkastreams.RequestSigning
import org.kae.twitterstreaming.credentials.TwitterCredentialsProvider

/**
  * Demo app to collect statistics from a Twitter stream using Akka HTTP and
  * FS2 streams.
  */
object AppUsingFs2
  extends App
    with RequestSigning
    with RequestBuilding {

  sys.error("Not yet implemented fully")

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
  }
}
