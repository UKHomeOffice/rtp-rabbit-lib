package uk.gov.homeoffice.rabbitmq

import uk.gov.homeoffice.json.JsonError

case class RabbitException(jsonError: JsonError) extends Exception(jsonError.error.getOrElse(""), jsonError.throwable.orNull)