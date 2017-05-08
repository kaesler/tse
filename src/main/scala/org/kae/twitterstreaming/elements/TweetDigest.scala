package org.kae.twitterstreaming.elements

/**
 * Digest of the content of a tweet.
 *
 * @param emojis the emojis found in the tweet text
 * @param hashTags the hashtags found in the tweet text
 * @param urlDomains the URL domains mentioned
 */
final case class TweetDigest(
    emojis: Set[Emoji],
    hashTags: Set[HashTag],
    urlDomains: Set[UrlDomain]
)
