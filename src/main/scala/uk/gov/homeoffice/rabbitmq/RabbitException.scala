package uk.gov.homeoffice.rabbitmq

import uk.gov.homeoffice.json.JsonError

case class RabbitException(val jsonError: JsonError) extends Exception(jsonError.error, jsonError.exception.orNull)

case class RetryException(cause: Exception) extends Exception

case class AlertException(cause: Exception) extends Exception