package boldradius.scala.collection

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{Matchers, WordSpec}

class MaskedSpec
  extends WordSpec
          with Matchers
          with LazyLogging {

  "Traversable factory" when {

    "in an out are empty" should {
      "mask empty" in {
        Masked(Nil, Nil) should be(MaskedEmpty)
      }
    }

    "in is non-empty and out is empty" should {
      val in = List(1, 2, 3)
      val exemplar = Masked(in, Nil)

      "mask none" in {
        exemplar should be(a[MaskedNone[_]])
      }

      "retain in" in {
        val MaskedNone(actual) = exemplar
        actual should be(in)
      }
    }

    "in is empty and out is non-empty" should {
      val out = List(1, 2, 3)
      val exemplar = Masked(Nil, out)

      "mask all" in {
        exemplar should be(a[MaskedAll[_]])
      }

      "retain out" in {
        val MaskedAll(actual) = exemplar
        actual should be(out)
      }
    }

    "in and out are non-empty" should {
      val in = List(1, 2, 3)
      val out = List(2, 3, 4)
      val exemplar = Masked(in, out)

      "mask some" in {
        exemplar should be(a[MaskedSome[_]])
      }

      "retain in and out" in {
        val MaskedSome(actualIn, actualOut) = exemplar
        val actual = (actualIn, actualOut)
        actual should be((in, out))
      }
    }

  }

}
