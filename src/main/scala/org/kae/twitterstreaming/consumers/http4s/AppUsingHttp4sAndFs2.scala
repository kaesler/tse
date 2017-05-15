package org.kae.twitterstreaming.consumers.http4s

import fs2.{Stream, Task}
import fs2.io.stdout
import fs2.text.{lines, utf8Encode}

import io.circe.Json
import jawnfs2._
import org.http4s._
import org.http4s.client.blaze._
import org.http4s.client.oauth1

import org.kae.twitterstreaming.credentials.{TwitterCredentials, TwitterCredentialsProvider}

/**
  * Demo app to collect statistics from a Twitter stream using Http4s and FS2 streams.
  * Code adapted from http://http4s.org/v0.17/streaming/.
  */
object AppUsingHttp4sAndFs2
  extends App {

  sys.error("Not yet implemented fully")

  // Note: fail here early and hard if no credentials.
  private val creds = TwitterCredentialsProvider.required()

  implicit val f = io.circe.jawn.CirceSupportParser.facade

  // Remember, this `Client` needs to be cleanly shutdown
  val client = PooledHttp1Client()

  val task: Task[Unit] = runc(creds)

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
  def stream(creds: TwitterCredentials)
    (req: Request): Stream[Task, Json] = for {
    sr  <- Stream.eval(sign(creds)(req))
    res <- client.streaming(sr)(resp => resp.body.chunks.parseJsonStream)
  } yield res

  /* Stream the sample statuses.
   * We map over the Circe `Json` objects to pretty-print them with `spaces2`.
   * Then we `to` them to fs2's `lines` and the to `stdout` `Sink` to print them.
   * Finally, when the stream is complete (you hit <crtl-C>), `shutdown` the `client`.
   */
  def runc(creds: TwitterCredentials): Task[Unit] = {
    val req = Request(
      Method.GET,
      Uri.uri("https://stream.twitter.com/1.1/statuses/sample.json"))

    // TODO: interpret the JSON to stats.

    stream(creds)(req)
      .map(_.spaces2)
      .through(lines)
      .through(utf8Encode)
      .to(stdout)
      .onFinalize(client.shutdown)
      .run
  }
}
