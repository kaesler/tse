package org.kae.twitterstreaming.streamcontents

import akka.http.scaladsl.model.Uri

/**
 * Digest of the content of a tweet.
 *
 * @param emojis the emojis found in the tweet text, duplicates permitted
 * @param hashTags the hashtags found in the tweet text, duplicates permitted
 * @param urlDomains the URL domains mentioned, duplicates permitted
 * @param hasPhoto has a URL to a photo
 */
final case class TweetDigest(
    uris: List[Uri],
    emojis: List[Emoji],
    hashTags: List[HashTag],
    urlDomains: List[UrlDomain],
    hasPhoto: Boolean
)
