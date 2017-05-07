package org.kae.twitterstreaming.elements

import scala.collection.JavaConverters._

import com.vdurmont.emoji.EmojiManager

/**
 * An Emoji.
 *
 * @param description its description
 * @param unicodeRepresentation its Unicode representation
 */
case class Emoji(
  description: String,
  unicodeRepresentation: String
)

object Emoji {

  private lazy val registry =
    EmojiManager.getAll.asScala
      .map { javaEmoji =>
        javaEmoji.getUnicode -> Emoji(javaEmoji.getDescription, javaEmoji.getUnicode) }
      .toMap

  /**
   * Get the [[Emoji]] for a string if there is one.
   *
   * @param s the [[String]]
   * @return the [[Emoji]] or None
   */
  def get(s: String): Option[Emoji] = registry.get(s)
}