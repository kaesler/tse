package org.kae.twitterstreaming.streamcontents

import scala.util.Try

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.NamedHost

import org.scalactic.TypeCheckedTripleEquals

trait UrlAnalysis extends TypeCheckedTripleEquals {

  /**
    * Try to parse a string to a Uri of the expected kind.
    *
    * @param s the string
    * @return a Uri, or None
    */
  def parseAsHttpUrl(s: String): Option[Uri] = {
    for {
      uri <- Try { Uri(s) }.toOption
      httpUri ← {
        if (uri.scheme === Uri.httpScheme(false)  || uri.scheme === Uri.httpScheme(true)) {
          Some(uri)
        } else {
          None
        }
      }
    } yield httpUri
  }

  /**
    * Try to extract a URL domain from a string after first trying to interpret it as an HTTP URL.
    *
    * @param s the string
    * @return the [[UrlDomain]] if it can be found, or None
    */
  def urlDomain(s: String): Option[UrlDomain] = {
    for {
      uri ← parseAsHttpUrl(s)
      host ← uri.authority.host.toOption
      urlDomain ← host match {

        case NamedHost(name) ⇒
          val nameComponents = name.split(".")
          if (nameComponents.length < 2) {
            None
          } else {
            val lastTwo = nameComponents.takeRight(2)
            Some(UrlDomain(s"${lastTwo(0)},${lastTwo(1)}"))
          }

        case _ ⇒ None
      }
    } yield urlDomain
  }
}

object UrlAnalysis extends UrlAnalysis
