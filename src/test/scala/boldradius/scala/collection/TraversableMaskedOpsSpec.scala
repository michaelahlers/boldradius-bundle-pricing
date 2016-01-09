package boldradius.scala.collection

import com.codahale.metrics.Timer
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{Matchers, WordSpec}

import scala.util.Random

class TraversableMaskedOpsSpec
  extends WordSpec
          with Matchers
          with LazyLogging {

  "Masked traversables" must {

    "exclude masked elements" in {

      val expected = MaskedSome(List("A", "B", "C"), List("A", "A", "A", "B", "B", "C"))
      val actual = List("A", "A", "A", "A", "B", "B", "B", "C", "C").masked(Map("A" -> 3, "B" -> 2, "C" -> 1))

      actual should be(expected)

    }

    "exclude masked elements (despite order)" in {

      val expected = MaskedSome(List("A", "B", "C").reverse, List("A", "A", "A", "B", "B", "C").reverse)
      val actual = List("A", "A", "A", "A", "B", "B", "B", "C", "C").reverse.masked(Map("A" -> 3, "B" -> 2, "C" -> 1))

      actual should be(expected)

    }

    "include all elements for empty mask" in {

      val expected = MaskedNone(List("A", "A", "A", "B", "B", "C"))
      val actual = expected.universe.masked(Map.empty)

      actual should be(expected)

    }

    "include all elements for disjoint mask" in {

      val expected = MaskedNone(List("A", "A", "A", "B", "B", "C"))
      val actual = expected.universe.masked(Map("X" -> 1, "Y" -> 2, "Z" -> 3))

      actual should be(expected)

    }

    "include all elements for partial mask" in {

      val expected = MaskedNone(List("A", "A", "A", "B", "B", "C"))
      val actual = expected.universe.masked(Map("A" -> 3, "B" -> 2, "C" -> 2))

      actual should be(expected)

    }

    "exclude all elements for total mask" in {

      val expected = MaskedAll(List("A", "A", "A", "B", "B", "C"))
      val actual = expected.universe.masked(Map("A" -> 3, "B" -> 2, "C" -> 1))

      actual should be(expected)

    }

  }

  "Empty masked traversables" must {

    "remain empty for empty mask" in {

      val expected = MaskedEmpty
      val actual = List.empty[String].masked(Map.empty)

      actual should be(expected)

    }

    "remain empty for any mask" in {

      val expected = MaskedEmpty
      val actual = List.empty[String].masked(Map("A" -> 3, "B" -> 2, "C" -> 2))

      actual should be(expected)

    }

  }

  "Performance" when {

    "streams are large" must {

      /** Returns nanoseconds. */
      def time(operation: => Any): Long = {
        val time = new Timer().time
        operation
        time.stop
      }

      "not exceed those of small collections" in {
        val exemplar: Stream[String] = Stream("A", "A", "A", "A", "B", "B", "B", "C", "C") ++ (0 until 10000000).map(_ => Random.nextString(1))
        val mask = Map("A" -> 3, "B" -> 2, "C" -> 1)

        val short = time(exemplar.take(20).masked(mask))
        val long = time(exemplar.masked(mask))

        val delta = Math.abs(short - long) / 1000F / 1000F
        val bound = 100F

        delta should be <= bound
      }

      /** It'd be suspicious for this to complete in nearly the same time. */
      "compare to tail matches" in {
        val exemplar = (0 until 10000000).map(_ => Random.nextString(1)) ++ Stream("A", "A", "A", "A", "B", "B", "B", "C", "C")
        val mask = Map("A" -> 3, "B" -> 2, "C" -> 1)

        val short = time(exemplar.takeRight(20).masked(mask))
        val long = time(exemplar.masked(mask))

        val delta = Math.abs(short - long) / 1000F / 1000F
        val bound = 100F

        delta should be > bound
      }

    }

  }

}
