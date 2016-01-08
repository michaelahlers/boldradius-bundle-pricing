package boldradius.catalog

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{Matchers, WordSpec}

class ItemSpec
  extends WordSpec
          with Matchers
          with LazyLogging {

  "Every new instance" should {

    "have a unique id. value" in {
      Item(SKU = "duplicate") shouldNot be(Item(SKU = "duplicate"))
    }

  }

}
