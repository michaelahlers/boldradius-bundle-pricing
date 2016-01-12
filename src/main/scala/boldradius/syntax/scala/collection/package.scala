package boldradius.syntax.scala

import boldradius.scala.collection.{TraversableCountByOps, TraversableMaskedOps}

import scala.collection.generic.CanBuildFrom
import scala.language.{higherKinds, implicitConversions}

package object collection {

  /**
   * Import to augment any [[scala.collection.Traversable]] with [[TraversableCountByOps count operations]].
   */
  implicit def withCountBy[A, C[A] <: Traversable[A]](c: C[A]): TraversableCountByOps[A, C] = new TraversableCountByOps(c)

  /**
   * Import to augment any [[scala.collection.Traversable]] with [[TraversableMaskedOps mask operations]].
   */
  implicit def withMasked[A, C[A] <: Traversable[A]](c: C[A])(implicit cbf: CanBuildFrom[C[A], A, C[A]]) = new TraversableMaskedOps(c)

}
