package org.kae.twitterstreaming.consumers.akkastreams

import scala.collection.JavaConverters._

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.{Authorization, RawHeader}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}

import org.scalactic.TypeCheckedTripleEquals
import com.github.scribejava.apis.TwitterApi
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.model.{OAuth1AccessToken, OAuthRequest, Verb}
import org.kae.twitterstreaming.credentials.{TwitterCredentials, TwitterCredentialsProvider}

/**
 * Signing Twitter requests by delegating to the Scribe library.
 */
trait RequestSigning
  extends TypeCheckedTripleEquals {

  /**
   * Sign an [[HttpRequest]] for Twitter by creating an equivalent [[OAuthRequest]],
   * signing it with Scribe, and adding the auth header produced to the [[HttpRequest]].
   *
   * @param req the [[HttpRequest]]
   * @param creds the [[TwitterCredentials]]
   * @return a new signed [[HttpRequest]]
   */
  protected def sign(req: HttpRequest, creds: TwitterCredentials): HttpRequest = {

    val AuthHeaderName = Authorization.name

    val surrogate = equivalentScribeRequest(req)

    val service = new ServiceBuilder()
      .apiKey(creds.consumerKey)
      .apiSecret(creds.consumerSecret)
      .build(TwitterApi.instance())

    service.signRequest(
      new OAuth1AccessToken(
        creds.accessToken,
        creds.accessTokenSecret),
      surrogate)
    val headers = surrogate.getHeaders.asScala
    val authHeaderValue = headers.getOrElse(AuthHeaderName, sys.error("Scribe signing failed"))

    req.withHeaders(
      req.headers
        // Remove any existing auth header and
        // add the one Scribe made.
        .filterNot(_.is(AuthHeaderName.toLowerCase))
        .+:(RawHeader(AuthHeaderName, authHeaderValue)))
  }

  private def equivalentScribeRequest(akkaReq: HttpRequest): OAuthRequest = {
    require(akkaReq.method === HttpMethods.GET)

    val urlWithoutQueryParams = akkaReq.uri.withQuery(Query.Empty).toString
    val queryParams = akkaReq.uri.query()

    val surrogate = new OAuthRequest(Verb.GET, urlWithoutQueryParams)
    queryParams foreach { case (name, value) ⇒
      surrogate.addQuerystringParameter(name, value)
    }
    surrogate
  }
}
