package org.kae.twitterstreaming.akka

import scala.concurrent.Future

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Framing, Source}
import akka.util.ByteString

/**
  * Demo app to collect statistics from a Twitter stream.
  */
object TwitterStreamConsumer
  extends App
  with RequestSigning
  with RequestBuilding {

  // TODO:
  //    - parse to JSON

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()

  import system.dispatcher

  private val streamUrl = "https://stream.twitter.com/1.1/statuses/sample.json" +
    "?stall_warnings=true"

  private val MaximumExpectedLineLength = 40000

  consumeResponse(
    signAndSend(
      Get(streamUrl)))
    // Note: for we terminate the ActorSystem and process if the response ends.
    .onComplete { _ ⇒ system.terminate() }

  private def signAndSend(req: HttpRequest): Source[ByteString, NotUsed] =
    Source.fromFutureSource(
      Http().singleRequest(sign(req))
        .map(_.entity.dataBytes))
      .mapMaterializedValue(_ ⇒ NotUsed)

  private def consumeResponse(
      byteStrings: Source[ByteString, NotUsed]
  ): Future[Done] =
    byteStrings
      // Split into lines,
      .via (
        Framing.delimiter(
         ByteString("\r\n"),
         maximumFrameLength = MaximumExpectedLineLength,
         allowTruncation = true))

      // This ensures that a failure due to exceeding MaximumExpectedLineLength
      // will be logged.
      .log("Twitter stream")

      // Convert to strings.
      .map(_.utf8String)

      .grouped(2)

      // For now just print.
      .runForeach(println)

  private def toChars(byteStrings: Source[ByteString, NotUsed]): Source[Char, NotUsed] =
    byteStrings.mapConcat { _.utf8String.toVector }

}
