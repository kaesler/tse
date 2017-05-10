package org.kae.twitterstreaming.consumers.akkastreams

import java.time.Instant

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.{Done, NotUsed}
import de.knutwalker.akka.stream.JsonStreamParser
import io.circe.jawn.CirceSupportParser
import org.kae.twitterstreaming.statistics.StatisticsAccumulator
import org.kae.twitterstreaming.streamcontents.{StallWarning, StreamElement, Tweet, TweetDigest}

/**
 * Demo app to collect statistics from a Twitter stream using Akka HTTP and
 * Akka Streams
 */
object AppUsingAkkaStreams
  extends App
  with RequestSigning
  with RequestBuilding {

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()

  import system.dispatcher

  private val logger = Logging.getLogger(system, this)

  private val streamUrl =
    "https://stream.twitter.com/1.1/statuses/sample.json" +
      "?stall_warnings=true"

  private val accumulator = new StatisticsAccumulator(Instant.now)

  consumeResponse(initiateResponse())
    // Note: for we terminate the ActorSystem and process if the response ends.
    .onComplete { _ ⇒
      println(accumulator.snapshot.asText)
      system.terminate()
    }

  private def initiateResponse(): Source[ByteString, NotUsed] =
    signAndSend(Get(streamUrl))

  private def signAndSend(req: HttpRequest): Source[ByteString, NotUsed] =
    Source.fromFutureSource(
      Http().singleRequest(sign(req))
        .map(_.entity.dataBytes))
      .mapMaterializedValue(_ ⇒ NotUsed)

  private def consumeResponse(
      byteStrings: Source[ByteString, NotUsed]
  ): Future[Done] = {

    toTweetDigests(byteStrings)

      // Run for a finite time.
      .takeWithin(1.minutes)

      // Ensure helpful error logging.
      .log("Twitter elements")

      // For now just print.
      .runForeach { digest => accumulator.accumulate(digest) }
  }

  private def toTweetDigests(
      byteStrings: Source[ByteString, NotUsed]): Source[TweetDigest, NotUsed] =

    byteStrings
      // ByteString -> Json
      .via {
        import CirceSupportParser._
        JsonStreamParser.flow
      }

      // Json -> StreamElement
      .async
      .map(StreamElement.apply)
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
      .mapAsyncUnordered(4) { tweet =>
        Future.successful(tweet.digest)
      }
}
