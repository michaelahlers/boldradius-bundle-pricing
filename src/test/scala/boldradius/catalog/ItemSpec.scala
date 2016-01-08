package boldradius.catalog

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json._

class ItemSpec
  extends WordSpec
          with Matchers
          with LazyLogging {

  "Every new instance" should {

    "have a unique id. value" in {
      Item(SKU = "duplicate") shouldNot be(Item(SKU = "duplicate"))
    }

  }

  "JSON serializers" should {

    "read and write without loss" in {
      val expected = Item(SKU = "ABC123")
      val actual = toJson(expected).validate[Item]
      actual should be(JsSuccess(expected))
    }

  }

}
