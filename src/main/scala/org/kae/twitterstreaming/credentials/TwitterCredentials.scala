package org.kae.twitterstreaming.credentials

/**
  * Strings needed for signing Twitter API requests.
  */
final case class TwitterCredentials (
  consumerKey: String,
  consumerSecret: String,
  accessToken: String,
  accessTokenSecret: String
)
