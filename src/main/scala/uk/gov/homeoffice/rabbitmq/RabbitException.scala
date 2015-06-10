package uk.gov.homeoffice.rabbitmq

case class RabbitException(t: Throwable, message: String = "Rabbit Error", data: Option[Any] = None) extends Exception(message, t)