package boldradius.scala.collection

import scala.annotation.tailrec
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.Builder
import scala.language.{higherKinds, postfixOps}
import scalaz.syntax.std.map._

/**
 * Masked operations wrapper. Provides functions that, given a pattern matched completely, masks elements from a [[scala.collection.Traversable traversable]]. Evaluation stops when the mask is satisfied or no more elements are left to evaluate. Given a lazy [[scala.collection.generic.CanBuildFrom collection builder]] (such as is used for [[scala.collection.immutable.Stream.StreamBuilder streams]]), this operation will be cheap—even for many elements. Understand the implications of whatever generic builder may apply, considering the size of the evaluated traversable.
 *
 * The following examples best illustrate usage and behavior:
 *
 * {{{
import boldradius.syntax.scala.collection._

val mask = Map('A' -> 3, 'B' -> 2, 'C' -> 1)

// Pattern matches some of the list.
assert(List('A', 'A', 'A', 'A', 'B', 'B', 'B', 'C', 'C').masked(mask) == MaskedSome(List('A', 'B', 'C'), List('A', 'A', 'A', 'B', 'B', 'C')))

// Pattern matches the entire list.
assert(List('A', 'A', 'A', 'B', 'B', 'C').masked(mask) == MaskedAll(List('A', 'A', 'A', 'B', 'B', 'C')))

// Pattern doesn't match entirely.
assert(List('A', 'A', 'A', 'B', 'B', 'X', 'Y', 'Z').masked(mask) == MaskedNone(List('A', 'A', 'A', 'B', 'B', 'X', 'Y', 'Z')))

// Masked an empty list.
assert(List.empty[Char].masked(mask) == MaskedEmpty)

// Masked an empty list with empty mask.
assert(List.empty[Char].masked(Map.empty) == MaskedEmpty)
 * }}}
 *
 * An identity function may be supplied to control matching behavior:
 *
 * {{{
import boldradius.catalog.Item
import boldradius.syntax.scala.collection._

val mask = Map("bread" -> 2, "margarine" -> 2)

val apple = Item(SKU = "apple")

val bread0 = Item(SKU = "bread")
val bread1 = Item(SKU = "bread")

val margarine0 = Item(SKU = "margarine")
val margarine1 = Item(SKU = "margarine")

assert(
  List(apple, bread0, bread1, margarine0, margarine1).masked(mask, _.SKU) ==
    MaskedSome(List(apple), List(bread0, bread1, margarine0, margarine1))
)
 * }}}
 */
class TraversableMaskedOps[A, C[A] <: Traversable[A]](c: C[A])(implicit cbf: CanBuildFrom[C[A], A, C[A]]) {

  def masked[Id](pattern: Map[Id, Int], id: A => Id): Masked[C[A]] = {

    @tailrec
    def consume(m: Map[Id, Int], cq: Traversable[A], ib: Builder[A, C[A]], ob: Builder[A, C[A]]): Masked[C[A]] =
      cq.headOption match {

        /** Got an element and it matches a rule in the pattern. Append it to the matches, decrement the rule--removing it if it's no longer applicable--and continue with the reduced pattern and queue tail. */
        case Some(ch) if m.contains(id(ch)) =>
          consume(
            m.alter(id(ch))(_.map(_ - 1).filter(0 <)),
            cq.tail,
            ib,
            ob += ch
          )

        /** Got an element to test but the pattern was empty (no rules remained). Append the whole queue (which includes the head element) to the remainders, and emit results from both builders. */
        case Some(ch) if m.isEmpty =>
          Masked((ib ++= cq).result, ob.result)

        /** Got an element to test and matches nothing in the pattern. Append it to the remainders, and continue with the queue tail. */
        case Some(ch) =>
          consume(m, cq.tail, ib += ch, ob)

        /** No more elements to test and the mask is totally empty, indicating it was either empty or all rules matched. Emit results from both builders. */
        case None if m.isEmpty =>
          Masked(ib.result, ob.result)

        /** But there remain rules in the mask, so it was incomplete. None or only parts of the pattern match this collection–emit the original. */
        case None =>
          Masked(c)

      }

    consume(pattern, c, cbf(c), cbf(c))

  }

  def masked(pattern: Map[A, Int]): Masked[C[A]] = masked(pattern.map({ case (k, v) => identity(k) -> v }), identity)

}
