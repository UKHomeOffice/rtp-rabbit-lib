package uk.gov.homeoffice.rabbitmq

import uk.gov.homeoffice.json.JsonError

class RabbitException(val jsonError: JsonError) extends Exception(jsonError.error, jsonError.exception.orNull)