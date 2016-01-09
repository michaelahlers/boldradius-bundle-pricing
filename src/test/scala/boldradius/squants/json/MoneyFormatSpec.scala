package boldradius.squants.json

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json._
import play.api.libs.json.{JsError, JsNumber, JsString, JsSuccess}
import squants.Money
import squants.market.{BTC, JPY, USD, XAU}

class MoneyFormatSpec
  extends WordSpec
          with Matchers
          with LazyLogging {

  "JSON serializers" should {

    val exemplars =
      USD(10) ::
        JPY(1200) ::
        XAU(50) ::
        BTC(50) ::
        Nil

    exemplars foreach { expected =>
      s"""read and write "$expected" without loss""" in {
        val actual = toJson(expected).validate[Money]
        actual should be(JsSuccess(expected))
      }
    }

    "report invalid serializations" in {
      JsString("bogus money").validate[Money] should be(a[JsError])
    }

    "report invalid types" in {
      JsNumber(123).validate[Money] should be(a[JsError])
    }

  }

}
