package org.kae.twitterstreaming.streamcontents

import org.scalatest.{FlatSpec, Matchers}

class UrlAnalysisTest
  extends FlatSpec
  with Matchers {

  "UrlAnalysis.parseAsHttpUrl" should "correctly reject invalid URLs" in {
    List(
      "",
      "foo",
      "/blah",
      "httpx://foo.com"

    ) foreach { s ⇒

      UrlAnalysis.parseAsHttpUrl(s) shouldBe None
    }
  }

  "UrlAnalysis.parseAsHttpUrl" should "correctly accept ivalid URLs" in {
    List(
      "https://",
      "http://kevinesler.com",
      "http://kevinesler.com/",
      "http://kevinesler.com/blah",
      "http://kevinesler.com/blah/baz--",
      "http://kevinesler.com/blah/baz?x=y&g=7"

    ) foreach { s ⇒

      UrlAnalysis.parseAsHttpUrl(s) shouldBe 'defined
    }
  }
}
