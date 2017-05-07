package org.kae.twitterstreaming.statistics

import java.time.Instant

import scala.collection.mutable.{Map => MMap}

import org.kae.twitterstreaming.elements.{Emoji, HashTag, TweetDigest, UrlDomain}

/**
 * For calculating cumulative statistics.
 * Contains mutable state and is not thread safe.
 */
trait StatisticsAccumulation {

  def startTime: Instant

  private var tweetCount = 0L
  private var tweetsContainingEmoji = 0L
  private var tweetsContainingUrl = 0L
  private var tweetsContainingPhotoUrl = 0L

  private val emojiOccurrences = MMap[Emoji, Long]()
  private val hashTagOccurrences = MMap[HashTag, Long]()
  private val urlDomainOccurrences = MMap[UrlDomain, Long]()

  /**
   * Accumulate the digest of a Tweet.
   *
   * @param digest the [[TweetDigest]]
   */
  def accumulate(digest: TweetDigest): Unit = {
    tweetCount += 1

    if (digest.emojis.nonEmpty) {
      tweetsContainingEmoji += 1
    }

    if (digest.urlDomains.nonEmpty) {
      tweetsContainingUrl += 1
    }

    addOrUpdate(digest.emojis, emojiOccurrences)
    addOrUpdate(digest.hashTags, hashTagOccurrences)
    addOrUpdate(digest.urlDomains, urlDomainOccurrences)
  }

  def statsAsText: String = {

    val secondsSinceStart = (Instant.now.toEpochMilli - startTime.toEpochMilli)/1000

    s"""
       |Statistics since $startTime:
       |  Total tweets received: $tweetCount
       |  Average tweets per second: ${tweetCount / secondsSinceStart}
       |  Top emojis: $topEmojis
       |  Tweets containing an emoji: $percentageTweetsContainingEmoji%
       |  Top hashtags: $topHashtags
       |  Tweets containing a URL: $percentageTweetsContainingUrl%
       |  Tweets containing a URL: $percentageTweetsContainingPhoto%
       |  Top URL domains: $topUrlDomains
       |
     """.stripMargin
  }

  private def addOrUpdate[T](ts: Iterable[T], mm: MMap[T, Long]): Unit = {
    ts foreach { t =>
      mm.get(t) match {
        case None =>
          mm += t -> 1L

        case Some(count) =>
          mm.update(t, count + 1)
      }
    }
  }

  private def topEmojis: String = ???
  private def topHashtags: String = ???
  private def topUrlDomains: String = ???

  private def percentageTweetsContainingEmoji: Int = ???
  private def percentageTweetsContainingUrl: Int = ???
  private def percentageTweetsContainingPhoto: Int = ???
}