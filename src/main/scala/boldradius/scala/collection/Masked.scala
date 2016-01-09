package boldradius.scala.collection

import scala.language.higherKinds

sealed trait Masked[+C]

object Masked {

  def apply[A, C[A] <: Traversable[A]](subset: C[A], matches: C[A]): Masked[C[A]] =
    (subset.nonEmpty, matches.nonEmpty) match {
      case (false, false) => Masked.empty
      case (true, false) => MaskedNone(subset)
      case (true, true) => MaskedSome(subset, matches)
      case (false, true) => MaskedAll(matches)
    }

  def apply[A, C[A] <: Traversable[A]](in: C[A]): Masked[C[A]] =
    if (in.nonEmpty) MaskedNone(in) else empty

  def empty[C]: Masked[C] = MaskedEmpty

}

/** Any mask (empty or not) applied to an empty traversable. */
case object MaskedEmpty extends Masked[Nothing]

/** */
case class MaskedNone[+C](universe: C) extends Masked[C]

case class MaskedSome[+C](subset: C, matches: C) extends Masked[C]

case class MaskedAll[+C](universe: C) extends Masked[C]
