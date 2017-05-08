package org.kae.twitterstreaming.statistics

import java.time.Instant

import scala.collection.mutable.{Map => MMap}

import org.kae.twitterstreaming.elements.{Emoji, HashTag, TweetDigest, UrlDomain}

/**
 * Methods for calculating cumulative statistics.
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

  /**
   * Capture current [[StatisticsSnapshot]].
   *
   * @return the [[StatisticsSnapshot]]
   */
  def snapshot: StatisticsSnapshot = {
    StatisticsSnapshot(
      startTime = startTime,
      endTime = Instant.now,
      totalTweets = ???,
      tweetsPerSecond = ???,
      emojiPrevalencePercentage = ???,
      urlPrevalencePercentage = ???,
      photoPrevalencePercentage = ???,
      topEmojis = ???,
      topHashtags = ???,
      topUrlDomains = ???
    )
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
