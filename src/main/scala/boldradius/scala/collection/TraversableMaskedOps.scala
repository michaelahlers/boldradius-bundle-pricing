package boldradius.scala.collection

import scala.annotation.tailrec
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.Builder
import scala.language.{higherKinds, postfixOps}
import scalaz.syntax.std.map._

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
