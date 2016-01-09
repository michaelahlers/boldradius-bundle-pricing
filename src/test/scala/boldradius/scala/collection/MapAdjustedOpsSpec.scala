package boldradius.scala.collection

import com.codahale.metrics.Timer
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{Matchers, WordSpec}

import scala.language.postfixOps
import scala.math

class MapAdjustedOpsSpec
  extends WordSpec
          with Matchers
          with LazyLogging {

  "Adjustments" must {

    "respect transforms" in {

      val expected = Map("a" -> 10, "b" -> -20, "c" -> 30)

      val actual =
        Map("a" -> 1, "b" -> 2, "c" -> 3)
          .adjusted("a", 10 *)
          .adjusted("b", _ - 22)
          .adjusted("c", 27 +)

      actual should be(expected)

    }

    "respect filters" in {

      val expected = Map("a" -> 10, "c" -> 30)

      val actual =
        Map("a" -> 1, "b" -> 2, "c" -> 3)
          .adjusted("a", 10 *)
          .adjusted("b", _ - 2, 0 <)
          .adjusted("c", 27 +, 30 >=)

      actual should be(expected)

    }

    "respect alternatives" in {

      val expected = Map("a" -> 10, "b" -> -20, "c" -> 30)

      val actual =
        Map("a" -> 1, "b" -> 2)
          .adjusted("a", 10 *)
          .adjusted("b", _ - 22)
          .adjusted("c", 27 +, alternative = Some(3))

      actual should be(expected)

    }

    "significantly out-perform naïve transform" in {

      /** Returns nanoseconds. */
      def time(operation: => Any): Long = {
        val time = new Timer().time
        operation
        time.stop
      }

      val sample: Map[Int, Char] = Map((0 until 1000000).map({ i => i -> ('A' + i % 26).toChar }): _*)

      val slow = time(sample transform { (k, v) => if (75 == k) v.toUpper else v })
      val fast = time(sample.adjusted(750, _.toUpper))

      math.log10(fast).toInt should be < math.log10(slow).toInt

    }

  }

}
