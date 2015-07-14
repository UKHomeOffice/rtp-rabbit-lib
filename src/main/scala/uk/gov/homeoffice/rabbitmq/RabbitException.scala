package uk.gov.homeoffice.rabbitmq

import uk.gov.homeoffice.json.JsonError

case class RabbitException(val jsonError: JsonError) extends Exception(jsonError.error, jsonError.throwable.orNull)

case class RetryThrowable(cause: Throwable) extends Throwable

case class AlertThrowable(cause: Throwable) extends Throwable