package org.kae.twitterstreaming.streamcontents

import scala.util.{Failure, Success}

import io.circe.jawn.CirceSupportParser
import org.scalatest.{FlatSpec, Matchers}

class StreamElementTest
  extends FlatSpec
  with Matchers {

  "StreamElement.isStallWarning" should "recognize its target" in {
    val text =
      """
        |{
        |  "warning":{
        |    "code":"FALLING_BEHIND",
        |    "message":"Your connection is falling behind and messages are being queued for delivery to you. Your queue is now over 60% full. You will be disconnected when the queue is full.",
        |    "percent_full": 60
        |  }
        |}
      """.stripMargin
    val json = CirceSupportParser.parseFromString(text) match {
      case Success(j) ⇒ j
      case Failure(_) ⇒ fail
    }
    StreamElement.isStallWarning(json) shouldBe true
  }

  "StreamElement.isTweet" should "recognize its target" in {
    val text =
      """
        |{
        |  "timestamp_ms": "1494083032666",
        |  "lang": "und",
        |  "filter_level": "low",
        |  "retweeted": false,
        |  "favorited": false,
        |  "entities": {
        |    "symbols": [],
        |    "user_mentions": [
        |      {
        |        "indices": [
        |          3,
        |          13
        |        ],
        |        "id_str": "754409294276227072",
        |        "id": 754409294276227072,
        |        "name": "Mijntje Frederiks",
        |        "screen_name": "Mijntje_F"
        |      },
        |      {
        |        "indices": [
        |          103,
        |          112
        |        ],
        |        "id_str": "484517229",
        |        "id": 484517229,
        |        "name": "Kyra Sivertson",
        |        "screen_name": "Okbabyyt"
        |      }
        |    ],
        |    "urls": [],
        |    "hashtags": [
        |      {
        |        "indices": [
        |          15,
        |          32
        |        ],
        |        "text": "okbabyto1million"
        |      },
        |      {
        |        "indices": [
        |          33,
        |          50
        |        ],
        |        "text": "okbabyto1million"
        |      },
        |      {
        |        "indices": [
        |          52,
        |          69
        |        ],
        |        "text": "okbabyto1million"
        |      },
        |      {
        |        "indices": [
        |          71,
        |          88
        |        ],
        |        "text": "okbabyto1million"
        |      }
        |    ]
        |  },
        |  "favorite_count": 0,
        |  "retweet_count": 0,
        |  "is_quote_status": false,
        |  "retweeted_status": {
        |    "lang": "und",
        |    "filter_level": "low",
        |    "retweeted": false,
        |    "favorited": false,
        |    "entities": {
        |      "symbols": [],
        |      "user_mentions": [
        |        {
        |          "indices": [
        |            88,
        |            97
        |          ],
        |          "id_str": "484517229",
        |          "id": 484517229,
        |          "name": "Kyra Sivertson",
        |          "screen_name": "Okbabyyt"
        |        }
        |      ],
        |      "urls": [],
        |      "hashtags": [
        |        {
        |          "indices": [
        |            0,
        |            17
        |          ],
        |          "text": "okbabyto1million"
        |        },
        |        {
        |          "indices": [
        |            18,
        |            35
        |          ],
        |          "text": "okbabyto1million"
        |        },
        |        {
        |          "indices": [
        |            37,
        |            54
        |          ],
        |          "text": "okbabyto1million"
        |        },
        |        {
        |          "indices": [
        |            56,
        |            73
        |          ],
        |          "text": "okbabyto1million"
        |        }
        |      ]
        |    },
        |    "favorite_count": 269,
        |    "retweet_count": 73,
        |    "is_quote_status": false,
        |    "contributors": null,
        |    "place": null,
        |    "coordinates": null,
        |    "geo": null,
        |    "user": {
        |      "notifications": null,
        |      "follow_request_sent": null,
        |      "following": null,
        |      "default_profile_image": false,
        |      "default_profile": true,
        |      "profile_banner_url": "https:\/\/pbs.twimg.com\/profile_banners\/754409294276227072\/1468700337",
        |      "profile_image_url_https": "https:\/\/pbs.twimg.com\/profile_images\/754409905898020864\/WQvEw_Mn_normal.jpg",
        |      "profile_image_url": "http:\/\/pbs.twimg.com\/profile_images\/754409905898020864\/WQvEw_Mn_normal.jpg",
        |      "profile_use_background_image": true,
        |      "profile_text_color": "333333",
        |      "profile_sidebar_fill_color": "DDEEF6",
        |      "profile_sidebar_border_color": "C0DEED",
        |      "profile_link_color": "1DA1F2",
        |      "profile_background_tile": false,
        |      "profile_background_image_url_https": "",
        |      "profile_background_image_url": "",
        |      "profile_background_color": "F5F8FA",
        |      "is_translator": false,
        |      "contributors_enabled": false,
        |      "lang": "nl",
        |      "geo_enabled": false,
        |      "time_zone": null,
        |      "utc_offset": null,
        |      "created_at": "Sat Jul 16 20:16:06 +0000 2016",
        |      "statuses_count": 11,
        |      "favourites_count": 33,
        |      "listed_count": 0,
        |      "friends_count": 21,
        |      "followers_count": 2,
        |      "verified": false,
        |      "protected": false,
        |      "description": "Bug buddie\ud83d\udc1e \/\/ Love Kyra, Oscar, Levi and Alaya\u2764\ufe0f @okbabyyt",
        |      "url": null,
        |      "location": null,
        |      "screen_name": "Mijntje_F",
        |      "name": "Mijntje Frederiks",
        |      "id_str": "754409294276227072",
        |      "id": 754409294276227072
        |    },
        |    "in_reply_to_screen_name": null,
        |    "in_reply_to_user_id_str": null,
        |    "in_reply_to_user_id": null,
        |    "in_reply_to_status_id_str": null,
        |    "in_reply_to_status_id": null,
        |    "truncated": false,
        |    "source": "<a href=\"http:\/\/twitter.com\/download\/iphone\" rel=\"nofollow\">Twitter for iPhone<\/a>",
        |    "text": "#okbabyto1million\n#okbabyto1million \n#okbabyto1million \n#okbabyto1million \n\u2764\ufe0f\u2764\ufe0f\u2764\ufe0f\u2764\ufe0f\u2764\ufe0f\u2764\ufe0f\n@Okbabyyt",
        |    "id_str": "860805048682635264",
        |    "id": 860805048682635264,
        |    "created_at": "Sat May 06 10:34:51 +0000 2017"
        |  },
        |  "contributors": null,
        |  "place": null,
        |  "coordinates": null,
        |  "geo": null,
        |  "user": {
        |    "notifications": null,
        |    "follow_request_sent": null,
        |    "following": null,
        |    "default_profile_image": false,
        |    "default_profile": false,
        |    "profile_banner_url": "https:\/\/pbs.twimg.com\/profile_banners\/3978808319\/1493507613",
        |    "profile_image_url_https": "https:\/\/pbs.twimg.com\/profile_images\/858459261105565696\/49lF62aj_normal.jpg",
        |    "profile_image_url": "http:\/\/pbs.twimg.com\/profile_images\/858459261105565696\/49lF62aj_normal.jpg",
        |    "profile_use_background_image": false,
        |    "profile_text_color": "000000",
        |    "profile_sidebar_fill_color": "000000",
        |    "profile_sidebar_border_color": "000000",
        |    "profile_link_color": "19CF86",
        |    "profile_background_tile": false,
        |    "profile_background_image_url_https": "https:\/\/abs.twimg.com\/images\/themes\/theme1\/bg.png",
        |    "profile_background_image_url": "http:\/\/abs.twimg.com\/images\/themes\/theme1\/bg.png",
        |    "profile_background_color": "000000",
        |    "is_translator": false,
        |    "contributors_enabled": false,
        |    "lang": "en",
        |    "geo_enabled": true,
        |    "time_zone": null,
        |    "utc_offset": null,
        |    "created_at": "Sat Oct 17 10:28:36 +0000 2015",
        |    "statuses_count": 1425,
        |    "favourites_count": 2803,
        |    "listed_count": 0,
        |    "friends_count": 277,
        |    "followers_count": 56,
        |    "verified": false,
        |    "protected": false,
        |    "description": null,
        |    "url": null,
        |    "location": "England, United Kingdom",
        |    "screen_name": "DybiuXoxo",
        |    "name": "Ell\ud83d\udc95",
        |    "id_str": "3978808319",
        |    "id": 3978808319
        |  },
        |  "in_reply_to_screen_name": null,
        |  "in_reply_to_user_id_str": null,
        |  "in_reply_to_user_id": null,
        |  "in_reply_to_status_id_str": null,
        |  "in_reply_to_status_id": null,
        |  "truncated": false,
        |  "source": "<a href=\"http:\/\/twitter.com\/download\/iphone\" rel=\"nofollow\">Twitter for iPhone<\/a>",
        |  "text": "RT @Mijntje_F: #okbabyto1million\n#okbabyto1million \n#okbabyto1million \n#okbabyto1million \n\u2764\ufe0f\u2764\ufe0f\u2764\ufe0f\u2764\ufe0f\u2764\ufe0f\u2764\ufe0f\n@Okbabyyt",
        |  "id_str": "860872750700924928",
        |  "id": 860872750700924928,
        |  "created_at": "Sat May 06 15:03:52 +0000 2017"
        |}     
      """.stripMargin
    val json = CirceSupportParser.parseFromString(text) match {
      case Success(j) ⇒ j
      case Failure(_) ⇒ fail
    }
    StreamElement.isTweet(json) shouldBe true
  }
}
