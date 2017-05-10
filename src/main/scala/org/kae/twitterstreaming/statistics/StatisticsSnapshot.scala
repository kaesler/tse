package org.kae.twitterstreaming.statistics

import java.time.Instant

import org.kae.twitterstreaming.streamcontents.{Emoji, HashTag, UrlDomain}

/**
 * Snapshot of accumulated statistics at a point in time.
 *
 * @param startTime start of period over which stats were collected
 * @param endTime end of period over which stats were collected
 * @param totalTweets total tweets seen
 * @param tweetsPerSecond rate of tweet arrival
 * @param emojiPrevalencePercentage percentage of tweeks with an emoji
 * @param urlPrevalencePercentage percentage of tweeks with a URL
 * @param photoPrevalencePercentage percentage of tweeks with a photo
 * @param topEmojis most popular emojis
 * @param topHashtags most popular hashtags
 * @param topUrlDomains most popular URL doains
 */
final case class StatisticsSnapshot(
    startTime: Instant,
    endTime: Instant,
    totalTweets: Long,
    tweetsPerSecond: Int,
    emojiPrevalencePercentage: Double,
    urlPrevalencePercentage: Double,
    photoPrevalencePercentage: Double,
    topEmojis: List[Emoji],
    topHashtags: List[HashTag],
    topUrlDomains: List[UrlDomain]
) {
  def asText: String = {

    f"""
       |Statistics from $startTime to $endTime:
       |  Total tweets received: $totalTweets
       |  Average tweets per second: $tweetsPerSecond
       |  Top emojis: ${topEmojis.map(_.description).mkString(",")}
       |  Tweets containing an emoji: $emojiPrevalencePercentage%.2f%%
       |  Top hashtags: ${topHashtags.map(_.asString).mkString(",")}
       |  Tweets containing a URL: $urlPrevalencePercentage%.2f%%
       |  Tweets containing a photo: $photoPrevalencePercentage%.2f%%
       |  Top URL domains: ${topUrlDomains.map(_.asString).mkString(",")}
       |
     """.stripMargin
  }
}
