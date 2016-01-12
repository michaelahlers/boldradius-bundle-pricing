package boldradius.scala.collection

import boldradius.syntax.scala.collection._
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{Matchers, WordSpec}

class TraversableCountByOpsSpec
  extends WordSpec
          with Matchers
          with LazyLogging {

  "Counts" must {

    "reflect identity" in {
      val expected = Map("a" -> 3, "b" -> 1, "c" -> 1, "C" -> 1)
      val actual = List("a", "a", "a", "b", "c", "C").counted

      actual should be(expected)
    }

    "reflect transformation" in {
      val expected = Map("A" -> 3, "B" -> 1, "C" -> 2)
      val actual = List("a", "a", "a", "b", "c", "C").countBy(_.toUpperCase)

      actual should be(expected)
    }

  }

}
