package org.kae.twitterstreaming.akka

import scala.concurrent.Future

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.{Done, NotUsed}
import de.knutwalker.akka.stream.JsonStreamParser

/**
  * Demo app to collect statistics from a Twitter stream.
  */
object TwitterStreamConsumer
  extends App
  with RequestSigning
  with RequestBuilding {

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()

  import system.dispatcher

  private val streamUrl = "https://stream.twitter.com/1.1/statuses/sample.json" +
    "?stall_warnings=true"

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
  ): Future[Done] = {

    byteStrings

      // Use JSON parser that can parse a stream of ByteStrings
      .via {
        import io.circe.jawn.CirceSupportParser._
        JsonStreamParser.flow
      }


      // TODO:
      // count warnings
      // skip non tweets
      .take(1000)

      // For now just print.
      .runForeach(_ => println("got one"))
  }
}
