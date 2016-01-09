package boldradius.scala.collection

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{Matchers, WordSpec}

import scala.language.postfixOps

class MapAdjustByOpsSpec
  extends WordSpec
          with Matchers
          with LazyLogging {

  "Adjustments" must {

    "respect transforms" in {

      val expected = Map("a" -> 10, "b" -> -20, "c" -> 30)

      val actual =
        Map("a" -> 1, "b" -> 2, "c" -> 3)
          .adjustBy("a", 10 *)
          .adjustBy("b", _ - 22)
          .adjustBy("c", 27 +)

      actual should be(expected)

    }

    "respect filters" in {

      val expected = Map("a" -> 10, "c" -> 30)

      val actual =
        Map("a" -> 1, "b" -> 2, "c" -> 3)
          .adjustBy("a", 10 *)
          .adjustBy("b", _ - 2, 0 <)
          .adjustBy("c", 27 +, 30 >=)

      actual should be(expected)

    }

    "respect alternatives" in {

      val expected = Map("a" -> 10, "b" -> -20, "c" -> 30)

      val actual =
        Map("a" -> 1, "b" -> 2)
          .adjustBy("a", 10 *)
          .adjustBy("b", _ - 22)
          .adjustBy("c", 27 +, alternative = Some(3))

      actual should be(expected)

    }

  }

}
