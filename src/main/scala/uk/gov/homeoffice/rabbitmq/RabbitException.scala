package uk.gov.homeoffice.rabbitmq

import org.json4s.JValue

class RabbitException(val t: Throwable, val message: String = "Rabbit Error") extends Exception(message, t)

class RabbitPublisherException(override val t: Throwable, val json: JValue) extends RabbitException(t, "Rabbit Publishing Error")