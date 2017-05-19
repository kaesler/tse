package org.kae.twitterstreaming.statistics

import java.time.{Instant, ZoneId}
import java.time.temporal.ChronoUnit

import org.scalactic.TypeCheckedTripleEquals

import org.kae.twitterstreaming.streamcontents.{Emoji, HashTag, TweetDigest, UrlDomain}


/**
 * Immutable class in which to accumulate statistics.
 *
 * @param startTime   start time
 * @param endTime end time
 * @param tweetCount total tweets seens
 * @param tweetsContainingEmoji tweets that had an Emoji
 * @param tweetsContainingUrl tweets that had a URL
 * @param tweetsContainingPhotoUrl tweets that had a photo URL
 * @param emojiOccurrences emojis and their occurrence counts
 * @param hashTagOccurrences hashtags and their occurrence counts
 * @param urlDomainOccurrences URL domains and their occurrence counts
 */
final case class CumulativeStatistics(
    startTime: Instant,
    endTime: Instant,
    tweetCount: Long,
    tweetsContainingEmoji: Long,
    tweetsContainingUrl: Long,
    tweetsContainingPhotoUrl: Long,
    emojiOccurrences: Map[Emoji, Long],
    hashTagOccurrences: Map[HashTag, Long],
    urlDomainOccurrences: Map[UrlDomain, Long]
) extends TypeCheckedTripleEquals {

  import CumulativeStatistics._

  /**
   * Accumulate the digest of a Tweet.
   *
   * @param digest the [[TweetDigest]]
   */
  def accumulate(digest: TweetDigest): CumulativeStatistics = {
    copy(
      endTime = Instant.now,

      tweetCount = tweetCount + 1,

      tweetsContainingEmoji = {
        if (digest.emojis.nonEmpty) {
          tweetsContainingEmoji + 1
        } else {
          tweetsContainingEmoji
        }
      },

      tweetsContainingUrl = {
        if (digest.urlDomains.nonEmpty) {
          tweetsContainingUrl + 1
        } else {
          tweetsContainingUrl
        }
      },

      tweetsContainingPhotoUrl = {
        if (digest.hasPhoto) {
          tweetsContainingPhotoUrl + 1
        } else {
          tweetsContainingPhotoUrl
        }
      },

      emojiOccurrences = updateCounts(digest.emojis, emojiOccurrences),
      hashTagOccurrences = updateCounts(digest.hashTags, hashTagOccurrences),
      urlDomainOccurrences = updateCounts(digest.urlDomains, urlDomainOccurrences)
    )
  }

  /**
   * @return a printable representation of the statistics
   */
  def asText: String = {

    def carefulPercentageOfTweets(numerator: Long): Double =
    // Avoid dividing by zero.
      if (tweetCount !== 0L) {
        (numerator * 100.0)/tweetCount
      } else {
        0L
      }

    val secondsElapsed = startTime.until(endTime, ChronoUnit.SECONDS)

    val tweetsPerSecond = {

      if (secondsElapsed === 0L) {
        0
      } else {
        (tweetCount / secondsElapsed).toInt
      }
    }

    val emojiPrevalencePercentage = carefulPercentageOfTweets(tweetsContainingEmoji)

    val urlPrevalencePercentage = carefulPercentageOfTweets(tweetsContainingUrl)

    val photoPrevalencePercentage = carefulPercentageOfTweets(tweetsContainingPhotoUrl)

    val topEmojis = topTenEntities(emojiOccurrences)
    val topHashtags = topTenEntities(hashTagOccurrences)
    val topUrlDomains = topTenEntities(urlDomainOccurrences)

    val topEmojisText = topEmojis
      .map { emoji =>
        s"'${emoji.unicodeRepresentation}': ${emoji.description}"
      }
      .mkString("\n    ","\n    ", "")

    f"""
       |Statistics for $secondsElapsed seconds from ${startTime.atZone(tz).toLocalTime} to ${endTime.atZone(tz).toLocalTime}:
       |  Total tweets received: $tweetCount
       |  Average tweets per second: $tweetsPerSecond
       |  Top emojis: $topEmojisText
       |  Tweets containing an emoji: $emojiPrevalencePercentage%.2f%%
       |  Top hashtags: ${topHashtags.map(tag ⇒ "#" + tag.asString).mkString("\n    ","\n    ", "")}
       |  Tweets containing a URL: $urlPrevalencePercentage%.2f%%
       |  Tweets containing a photo: $photoPrevalencePercentage%.2f%%
       |  Top URL domains: ${topUrlDomains.map(_.asString).mkString("\n    ","\n    ", "")}
     """.stripMargin
  }

  private def topTenEntities[E](map: Map[E, Long]): List[E] = {
    map
      .toList
      .sortBy(- _._2)
      .take(10)
      .map(_._1)
  }

  private def updateCounts[T](ts: Iterable[T], m: Map[T, Long]): Map[T, Long] = {
    ts.foldLeft(m) { (mapBefore, t) =>
      mapBefore.updated(
        t,
        mapBefore.get(t).map( _ + 1).getOrElse(1)
      )
    }
  }
}

object CumulativeStatistics {
  private val tz = ZoneId.systemDefault()

  /**
   * @return an empty instance
   */
  def empty: CumulativeStatistics = CumulativeStatistics(
    startTime = Instant.now,
    endTime = Instant.now,
    tweetCount = 0L,
    tweetsContainingEmoji = 0L,
    tweetsContainingUrl = 0L,
    tweetsContainingPhotoUrl = 0L,
    emojiOccurrences = Map.empty[Emoji, Long],
    hashTagOccurrences = Map.empty[HashTag, Long],
    urlDomainOccurrences = Map.empty[UrlDomain, Long]
  )
}
