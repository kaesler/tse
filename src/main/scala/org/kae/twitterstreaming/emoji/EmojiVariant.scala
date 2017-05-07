package org.kae.twitterstreaming.emoji

/**
 * An Emoji variant is a sequence of Unicode codepoints.
 * Many variants can encode a single Emoji.
 *
 * @param codePoints the codepoints
 */
case class EmojiVariant(
    codePoints: Vector[Int]
) {
  private lazy val asJavaString = {
    val sb = new StringBuilder
    sb ++ codePoints.map(Character.toChars)
    sb.result()
  }

  def toJavaString: String = asJavaString
}
