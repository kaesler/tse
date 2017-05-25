package org.kae.twitterstreaming.credentials

import java.io.{File, FileInputStream}
import java.util.Properties

import scala.util.Try

object TwitterCredentialsProvider {

  private val relPath = ".twitter_api/credentials"

  /**
    * Try to find [[TwitterCredentials]] from a file at
    * ~/.twitter_api/credentials
    * @return [[TwitterCredentials]] if found, otherwise None
    */
  def apply(): Option[TwitterCredentials] = {
    def getRequiredProperty(props: Properties, name: String): String =
      props.getProperty(name)
    Try {
        val homeDir = sys.props("user.home")
        val credsFile = new File(homeDir, relPath)
        require(credsFile.isFile)

        val props = new Properties

        props.load(new FileInputStream(credsFile))

        TwitterCredentials(
          consumerKey = getRequiredProperty(props, "consumerKey"),
          consumerSecret = getRequiredProperty(props, "consumerSecret"),
          accessToken = getRequiredProperty(props, "accessToken"),
          accessTokenSecret = getRequiredProperty(props, "accessTokenSecret")
        )
      }.toOption
  }

  /**
    * Look for Twitter credentials in the expected place and fail if not found
    * @return the [[TwitterCredentials]]
    */
  def required(): TwitterCredentials = apply() match {
    case Some(c) ⇒ c
    case None ⇒ sys.error(s"Twitter credentials not found in ~/$relPath")
  }
}
