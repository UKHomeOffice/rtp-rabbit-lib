package uk.gov.homeoffice.rabbitmq

import java.io.IOException
import scala.util.control.NonFatal
import org.json4s.JValue
import org.scalactic.{Or, Bad}
import uk.gov.homeoffice.json.JsonError

object RabbitException {
  // TODO Scaladoc
  def bad[G](json: JValue, pf: JValue => PartialFunction[Throwable, G Or JsonError] = (j: JValue) => PartialFunction.empty[Throwable, G Or JsonError]) = {
    val defaultPF = PartialFunction[Throwable, G Or JsonError] {
      case e: IOException => Bad(JsonError(json, e.getMessage, Some(RetryThrowable(e))))
      case NonFatal(n) => Bad(JsonError(json, n.getMessage, Some(n)))
      case t => Bad(JsonError(json, t.getMessage, Some(AlertThrowable(t))))
    }

    pf(json) orElse defaultPF
  }
}

case class RabbitException(jsonError: JsonError) extends Exception(jsonError.error, jsonError.throwable.orNull)

case class RetryThrowable(cause: Throwable) extends Throwable

case class AlertThrowable(cause: Throwable) extends Throwable