package org.kae.twitterstreaming.streamcontents

import io.circe.Json
import io.circe.optics.JsonPath._

/**
 * Base trait for all elements in the stream from Twitter.
 */
sealed trait StreamElement

/**
 * A Tweet.
 *
 * @param json the [[Json]]
 */
final case class Tweet(json: Json) extends StreamElement {
  def digest: TweetDigest = {
    TweetDigest(
      text,
      Nil,
      Nil,
      Nil
    )
  }
  private def text: String = {
    val optic = root.text.string
    optic.getOption(json).getOrElse("")
  }
}

/**
 * A stall warning.
 */
case object StallWarning extends StreamElement

/**
 * Any other payload.
 */
case object UninterestingStreamElement extends StreamElement

object StreamElement {
  /**
   * Create an instance appropriate to to the shape of the Json.
   *
   * @param json the [[Json]]
   * @return the instance
   */
  def apply(json: Json): StreamElement = {
    if (isTweet(json)) {
      Tweet(json)
    } else if (isStallWarning(json)) {
      StallWarning
    } else {
      UninterestingStreamElement
    }
  }

  /**
   * @return true iff json corresponds to a stall warning
   * @param json the [[Json]]
   */
  def isStallWarning(json: Json): Boolean = {
    val optic = root.warning.code.string
    optic.getOption(json).contains("FALLING_BEHIND")
  }

  /**
   * @return true iff the json corresponds to a Tweet.
   * @param json the [[Json]]
   */
  def isTweet(json: Json): Boolean = {
    val optic = root.retweeted.boolean
    optic.getOption(json).isDefined
  }
}