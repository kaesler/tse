package org.kae.twitterstreaming.statistics

import java.time.{Instant, ZoneId}

import cats.Monoid
import cats.implicits._

import org.kae.twitterstreaming.streamcontents.{Emoji, HashTag, UrlDomain}

/**
 * Immutable class in which to accumulate statistics.
 *
 * @param earliestTime   start time
 * @param latestTime end time
 * @param tweetCount total tweets seens
 * @param tweetsContainingEmoji tweets that had an Emoji
 * @param tweetsContainingUrl tweets that had a URL
 * @param tweetsContainingPhotoUrl tweets that had a photo URL
 * @param emojiOccurrences emojis and their occurrence counts
 * @param hashTagOccurrences hashtags and their occurrence counts
 * @param urlDomainOccurrences URL domains and their occurrence counts
 */
final case class Statistics(
    earliestTime: Option[Instant],
    latestTime: Option[Instant],
    tweetCount: Long,
    tweetsContainingEmoji: Long,
    tweetsContainingUrl: Long,
    tweetsContainingPhotoUrl: Long,
    emojiOccurrences: Map[Emoji, Long],
    hashTagOccurrences: Map[HashTag, Long],
    urlDomainOccurrences: Map[UrlDomain, Long]
) {

  import Statistics._

  /**
    * @return true iff the instance is not empty
    */
  def nonEmpty: Boolean = tweetCount =!= 0

  /**
   * @return true iff the stats are for a non-zero interval of time
   */
  def isForInterval: Boolean = (earliestTime, latestTime) match {
    case (Some(start), Some(end)) => end.isAfter(start)
    case _ => false
  }

  /**
   * A method that combines two instances in such a way as to satisfy the rules
   * for a commutative monoid.
   *
   * @param that the other instance
   * @return the combination of the two sets of statistics
   */
  def combine(that: Statistics): Statistics = {

    def combineOccurrences[T](
        l: Map[T, Long],
        r: Map[T, Long]): Map[T, Long] = {

      r.foldLeft(l) { case (mapToUpdate, (t, countIncrement)) =>
        val newValue =
          l.get(t)
            .map { oldCount => oldCount + countIncrement }
            .getOrElse(countIncrement)
        mapToUpdate.updated(t, newValue)
      }
    }

    Statistics(
      earliestTime = (this.earliestTime.toList ++ that.earliestTime.toList)
        .sortBy(_.toEpochMilli)
        .headOption,
      latestTime = (this.earliestTime.toList ++ that.earliestTime.toList)
        .sortBy(- _.toEpochMilli)
        .headOption,
      tweetCount = this.tweetCount + that.tweetCount,
      tweetsContainingEmoji = this.tweetsContainingEmoji + that.tweetsContainingEmoji,
      tweetsContainingUrl = this.tweetsContainingUrl + that.tweetsContainingUrl,
      tweetsContainingPhotoUrl = this.tweetsContainingPhotoUrl + that.tweetsContainingPhotoUrl,
      emojiOccurrences =
        combineOccurrences(this.emojiOccurrences, that.emojiOccurrences),
      hashTagOccurrences =
        combineOccurrences(this.hashTagOccurrences, that.hashTagOccurrences),
      urlDomainOccurrences =
        combineOccurrences(this.urlDomainOccurrences, that.urlDomainOccurrences)
    )
  }

  /**
   * @return a printable representation of the statistics
   */
  def asText: String = {

    def carefulPercentageOfTweets(numerator: Long): Double =
    // Avoid dividing by zero.
      if (tweetCount =!= 0L) {
        (numerator * 100.0)/tweetCount
      } else {
        0L
      }

    val secondsElapsed = (earliestTime, latestTime) match {
      case (Some(start), Some(end)) => (end.toEpochMilli - start.toEpochMilli)/1000

      case _ => 0L
    }


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

    val header = {
      if (isForInterval) {
        s"""Statistics for $secondsElapsed seconds from ${earliestTime.get.atZone(tz).toLocalTime} to ${latestTime.get.atZone(tz).toLocalTime}:"""
      } else {
        s"""Statistics:"""

      }
    }
    f"""
       |$header
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
}

object Statistics {

  implicit val statisticsMonoid = new Monoid[Statistics] {
    override val empty: Statistics = Empty

    override def combine(
        x: Statistics,
        y: Statistics): Statistics = x combine y
  }

  private val tz = ZoneId.systemDefault()

  val Empty: Statistics = Statistics(
    None,
    None,
    0L,
    0L,
    0L,
    0L,
    Map.empty,
    Map.empty,
    Map.empty
  )
}


