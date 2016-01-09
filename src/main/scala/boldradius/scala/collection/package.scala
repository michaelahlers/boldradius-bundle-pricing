package boldradius.scala

import scala.language.implicitConversions

package object collection {

  /** Import this to provide convenient augmentation of [[scala.collection.Map]] with [[MapAdjustByOps.adjustBy]]. */
  implicit def withAdjustBy[K, V](m: Map[K, V]): MapAdjustByOps[K, V] = new MapAdjustByOps(m)

}
