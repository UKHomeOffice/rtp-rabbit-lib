package uk.gov.homeoffice.rabbitmq

import java.io.IOException
import scala.util.control.NonFatal
import uk.gov.homeoffice.json.JsonError

// TODO Scaladoc
trait ErrorPolicy {
  def enforce: PartialFunction[JsonError, JsonError with ErrorType]
}

object DefaultErrorPolicy extends DefaultErrorPolicy

trait DefaultErrorPolicy extends ErrorPolicy {
  val enforce: PartialFunction[JsonError, JsonError with ErrorType] = {
    case JsonError(json, error, throwable @ Some(t: IOException)) => new JsonError(json, error, throwable) with Retry
    case JsonError(json, error, throwable @ Some(NonFatal(_))) => new JsonError(json, error, throwable) with Default
    case JsonError(json, error, None) => new JsonError(json, error, None) with Default
    case JsonError(json, error, throwable) => new JsonError(json, error, throwable) with Alert
  }
}

trait ErrorType

trait Alert extends ErrorType

trait Retry extends ErrorType

trait Default extends ErrorType