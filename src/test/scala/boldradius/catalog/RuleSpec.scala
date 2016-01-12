package boldradius.catalog

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json._
import squants.market.USD

class RuleSpec
  extends WordSpec
          with Matchers
          with LazyLogging {

  "JSON serializers" should {

    "read and write without loss" in {
      val expected = Rule(SKUs = List("ABC123", "DEF456"), cost = USD(10))
      val actual = toJson(expected).validate[Rule]
      actual should be(JsSuccess(expected))
    }

  }

  "Every new instance" should {

    "have a unique id. value" in {
      Rule(SKUs = Nil, cost = USD(10)) shouldNot be(Rule(SKUs = Nil, cost = USD(10)))
    }

  }

}
