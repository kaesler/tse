package org.kae.twitterstreaming.streamcontents

/**
 * Digest of the content of a tweet.
 *
 * @param emojis the emojis found in the tweet text, duplicates permitted
 * @param hashTags the hashtags found in the tweet text, duplicates permitted
 * @param urlDomains the URL domains mentioned, duplicates permitted
 */
final case class TweetDigest(
    emojis: List[Emoji],
    hashTags: List[HashTag],
    urlDomains: List[UrlDomain]
)
