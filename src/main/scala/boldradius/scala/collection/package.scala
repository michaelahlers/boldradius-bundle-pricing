package boldradius.scala

import scala.language.implicitConversions

package object collection {

  /** Import this to provide convenient augmentation of [[scala.collection.Map]] with [[MapAdjustedOps.adjusted]]. */
  implicit def withAdjusted[K, V](m: Map[K, V]): MapAdjustedOps[K, V] = new MapAdjustedOps(m)

}
