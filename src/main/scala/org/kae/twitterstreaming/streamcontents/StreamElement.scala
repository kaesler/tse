package org.kae.twitterstreaming.streamcontents

import java.time.Instant

import scala.collection.JavaConverters._

import akka.http.scaladsl.model.Uri

import com.vdurmont.emoji.EmojiParser
import io.circe.Json
import io.circe.optics.JsonPath._

import org.kae.twitterstreaming.statistics.Statistics

/**
 * Base trait for all elements in the stream from Twitter.
 */
sealed trait StreamElement

/**
 * A Tweet.
 *
 * @param json the [[Json]]
 */
final case class Tweet(json: Json)
  extends StreamElement
  with UrlAnalysis {

  /**
    * @return a [[Statistics]] instance for just this single [[Tweet]].
    */
  def statistics: Statistics = {

    def occurrencesInList[T](ts: List[T]): Map[T, Long] =
      ts
        .groupBy(identity)
        .map { case (t, occurrences) ⇒
          t -> occurrences.size.toLong
        }

    Statistics(
      earliestTime = Some(Instant.now),
      latestTime = Some(Instant.now),
      tweetCount = 1L,
      tweetsContainingEmoji = emojis.headOption.size,
      tweetsContainingUrl = urls.headOption.size,
      tweetsContainingPhotoUrl = {
        if (containsPhotoReference) {
          1L
        } else {
          0L
        }
      },
      emojiOccurrences = occurrencesInList(emojis),
      hashTagOccurrences = occurrencesInList(hashTags),
      urlDomainOccurrences = occurrencesInList(urlDomains)
    )
  }

  /**
    * Extract a digest of the tween
    *
    * @return the [[TweetDigest]]
    */
  def digest: TweetDigest = {
    TweetDigest(
      urls,
      emojis,
      hashTags,
      urlDomains,
      containsPhotoReference
    )
  }

  /**
    * @return the text of the tweet
    */
  def text: String = {
    val optic = root.text.string
    optic.getOption(json).getOrElse("")
  }

  /**
    * @return the URLs in the tweet
    */
  def urls: List[Uri] =
    root.entities.urls.each.expanded_url.string.getAll(json)
      .flatMap { s ⇒
        parseAsHttpUrl(s).toList
      }

  /**
   * @return true iff the Tweet contains a URL that refers to a photo
   */
  def containsPhotoReference: Boolean = urls exists refersToPhoto

  /**
    * @return the [[UrlDomain]]s in the tweet.
    */
  def urlDomains: List[UrlDomain] = for {
    s ← root.entities.urls.each.expanded_url.string.getAll(json)
    domain ← urlDomain(s).toList
  } yield domain

  /**
    * @return the [[HashTag]]s in the tweet
    */
  def hashTags: List[HashTag] = for {
    s ← root.entities.hashtags.each.text.string.getAll(json)
    if s.nonEmpty
  } yield HashTag(s)

  /**
    * @return the [[Emoji]]s in the tweet
    */
  def emojis: List[Emoji] = for {
    s ← EmojiParser.extractEmojis(text).asScala.toList
    emoji ← Emoji.get(s).toList
  } yield emoji
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