package org.kae.twitterstreaming.statistics

import java.time.Instant
import java.time.temporal.ChronoUnit

import scala.collection.mutable.{Map ⇒ MMap}

import org.kae.twitterstreaming.streamcontents.{Emoji, HashTag, TweetDigest, UrlDomain}

/**
 * Mutable class in which to accumulate statistics,
 * Not thread-safe.
 */
@SuppressWarnings(
  Array(
    "org.wartremover.warts.Var",
    "org.wartremover.warts.MutableDataStructures"))
class MutableCumulativeStatistics(startTime: Instant) {

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

    if (digest.hasPhoto) {
      tweetsContainingPhotoUrl += 1
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
    val secondsElapsed = startTime.until(Instant.now, ChronoUnit.SECONDS)
    StatisticsSnapshot(
      startTime = startTime,
      endTime = Instant.now,
      totalTweets = tweetCount,

      tweetsPerSecond = (tweetCount/secondsElapsed).toInt,

      emojiPrevalencePercentage =
        (tweetsContainingEmoji * 100.0)/tweetCount,

      urlPrevalencePercentage =
        (tweetsContainingUrl * 100.0)/tweetCount,

      photoPrevalencePercentage =
        (tweetsContainingPhotoUrl * 100.0)/tweetCount,

      topEmojis = topTenEntities(emojiOccurrences),
      topHashtags = topTenEntities(hashTagOccurrences),
      topUrlDomains = topTenEntities(urlDomainOccurrences)
    )
  }

  private def topTenEntities[E](map: MMap[E, Long]): List[E] = {
    map
      .toList
      .sortBy(- _._2)
      .take(10)
      .map(_._1)
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
}
