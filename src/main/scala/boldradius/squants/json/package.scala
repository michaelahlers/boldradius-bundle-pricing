package boldradius.squants

import boldradius.json.PartialFormat
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsString, JsSuccess}
import squants.market.Money

package object json {

  implicit object MoneyFormat extends PartialFormat[Money] {

    override def partialReads = {

      case JsString(value) =>
        Money(value)
          .map(JsSuccess(_))
          .getOrElse(JsError(ValidationError("error.expected.money")))

      case value =>
        JsError(ValidationError("error.expected.jsstring"))

    }

    override def partialWrites = {

      case money: Money =>
        JsString(money.toString)

    }

  }

}
