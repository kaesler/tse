package org.kae.twitterstreaming.streamcontents

import org.scalatest.{FlatSpec, Matchers}

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
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

  "UrlAnalysis.parseAsHttpUrl" should "correctly accept valid URLs" in {
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

  "UrlAnalysis.urlDomain" should "correctly reject invalid cases" in {
    List(
      "/kevinesler.com/",
      "https://.com/",
      "https://com/"

    ) foreach { s ⇒

      UrlAnalysis.urlDomain(s) shouldBe None
    }
  }
  "UrlAnalysis.urlDomain" should "correctly accept valid cases" in {
    List(
      "http://kevinesler.com/"

    ) foreach { s ⇒

      UrlAnalysis.urlDomain(s) shouldBe 'defined
    }
  }

  "UrlAnalysis.urlDomain" should "correctly extract the domain part of the hostname" in {
    UrlAnalysis.urlDomain("http://a.b.c") shouldBe Some(UrlDomain("b.c"))
    UrlAnalysis.urlDomain("http://1.2.3.4.5.6.7.a.b.c") shouldBe Some(UrlDomain("b.c"))
  }
}
