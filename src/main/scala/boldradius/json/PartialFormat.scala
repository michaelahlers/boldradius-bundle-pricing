package boldradius.json

import play.api.libs.json.{Format, JsError, JsResult, JsValue}

/**
 * Makes writing [[play.api.libs.json.Format]] implementations more concise.
 *
 * {{{
new PartialFormat[BigDecimal] {
  override def partialReads = {
    case JsNumber(v) => JsSuccess(v)
    case _ => JsError(ValidationError("error.expected.jsstring"))
  }

  override def partialWrites = {
    case v => JsNumber(v)
  }
}
 * }}}
 *
 * @tparam T See [[play.api.libs.json.Format]].
 */
trait PartialFormat[T] extends Format[T] {

  def partialReads: PartialFunction[JsValue, JsResult[T]]

  def partialWrites: PartialFunction[T, JsValue]

  final def writes(t: T): JsValue = partialWrites(t)

  final def reads(json: JsValue) = partialReads.lift(json).getOrElse(JsError( s"""Unsupported JSON value "$json".""""))

}
