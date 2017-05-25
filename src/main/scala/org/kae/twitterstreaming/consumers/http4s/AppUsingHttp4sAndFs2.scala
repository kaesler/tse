package org.kae.twitterstreaming.consumers.http4s

import fs2.{Stream, Task}

import io.circe.Json
import jawnfs2._
import org.http4s._
import org.http4s.client.blaze._
import org.http4s.client.oauth1

import org.kae.twitterstreaming.credentials.{TwitterCredentials, TwitterCredentialsProvider}
import org.kae.twitterstreaming.statistics.Statistics
import org.kae.twitterstreaming.streamcontents.{StallWarning, StreamElement, Tweet}

/**
  * Demo app to collect statistics from a Twitter stream using Http4s and FS2 streams.
  * Code adapted from http://http4s.org/v0.17/streaming/.
  */
object AppUsingHttp4sAndFs2
  extends App {

  // Note: fail here early and hard if no credentials.
  private val creds = TwitterCredentialsProvider.required()

  private implicit val facade = io.circe.jawn.CirceSupportParser.facade

  // Remember, this `Client` needs to be cleanly shutdown
  private val client = PooledHttp1Client()

  private val task: Task[Unit] = runFreshPipeline(creds, Statistics.Empty)

  // TODO: How to recover when response is interrupted by network error?

  task.unsafeRun

  /////////////////////////////////////////////////////////

  // OAuth signing is an effect due to generating a nonce for each `Request`.
  private def sign(creds: TwitterCredentials)
    (req: Request): Task[Request] = {

    val consumer = oauth1.Consumer(creds.consumerKey, creds.consumerSecret)
    val token = oauth1.Token(creds.accessToken, creds.accessTokenSecret)
    oauth1.signRequest(req, consumer, callback = None, verifier = None, token = Some(token))
  }

  // Sign the incoming `Request`, stream the `Response`, and `parseJsonStream` the `Response`.
  // `sign` returns a `Task`, so we need to `Stream.eval` it to use a for-comprehension.
  private def stream(creds: TwitterCredentials)
    (req: Request): Stream[Task, Json] = for {
    sr  <- Stream.eval(sign(creds)(req))
    res <- client.streaming(sr)(resp => resp.body.chunks.parseJsonStream)
  } yield res

  private def runFreshPipeline(
      creds: TwitterCredentials,
      initialStats: Statistics
  ): Task[Unit] = {

    val req = Request(
      Method.GET,
      Uri.uri("https://stream.twitter.com/1.1/statuses/sample.json"))

    stream(creds)(req)
      .map(StreamElement.apply)
      .map {
        case StallWarning ⇒
          // logger.warning("Stall warning occurred")
          StallWarning
        case other ⇒ other
      }

      // StreamElement -> Tweet
      .collect[Tweet] { case t: Tweet => t }

      // Tweet -> Statistics
      // TODO: how to get 4 running concurrently at once here ?
      .map(_.statistics)

      // Statistics -> Statistics (total)
      .scan(initialStats) { (accumulatedStats, singleTweetStats) =>
        accumulatedStats combine singleTweetStats
      }

      // TODO:
      // Strategy:
      //   - in lieu of a conflate() combinator, pipe through a circular buffer
      //     of length 1
      //   - zip the output of that with clock ticks at the desired cadence
      //   - print

      .map(_.asText)

      // TODO: This seems wrong.
      .map(println)

      .onFinalize(client.shutdown)
      .run
  }
}
