package org.kae.twitterstreaming.streamcontents

/**
 * Type for a hashtag found in a tweet.
 *
 * @param asString the string representation, excluding the '#'
 */
final case class HashTag(asString: String) extends AnyVal
