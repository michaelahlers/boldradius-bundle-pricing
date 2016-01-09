package boldradius.scala.collection

import scala.language.higherKinds

/**
 * Represents the outcome of [[TraversableMaskedOps masking]] a [[scala.collection.Traversable]] using a given pattern.
 *
 * - Any pattern (empty or not) matched against an empty [[scala.collection.Traversable stream]] is [[MaskedEmpty]].
 * - Nothing masked in a [[scala.collection.Traversable stream]] is [[MaskedNone]].
 * - Partial elements masked in a [[scala.collection.Traversable stream]] is [[MaskedSome]].
 * - All elements masked in a [[scala.collection.Traversable stream]] is [[MaskedAll]].
 */
sealed trait Masked[+C]

/**
 * @see [[Masked]]
 */
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

/** No elements were supplied to be masked. */
case object MaskedEmpty extends Masked[Nothing]

/** _No_ elements were masked by the pattern (making this type's value the universal set). */
case class MaskedNone[+C](universe: C) extends Masked[C]

/** A partial match was achieved (making this type's value a subset, with matching elements provided for convenience). */
case class MaskedSome[+C](subset: C, matches: C) extends Masked[C]

/** _All_ elements were masked by the pattern (making this type's value the universal set). */
case class MaskedAll[+C](universe: C) extends Masked[C]
