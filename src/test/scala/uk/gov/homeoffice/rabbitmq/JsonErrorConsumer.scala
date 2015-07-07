package uk.gov.homeoffice.rabbitmq

import org.json4s.native.JsonMethods._
import com.rabbitmq.client.{AMQP, Envelope, DefaultConsumer, Channel}
import uk.gov.homeoffice.json.JsonError

trait JsonErrorConsumer extends WithQueue {
  override def errorQueue(channel: Channel): String = {
    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]) = {
        super.handleDelivery(consumerTag, envelope, properties, body)
        jsonError {
          val extractedJsonError = parse(new String(body))
          JsonError(extractedJsonError, error = (extractedJsonError \ "error").extract[String])
        }
      }
    }

    channel.basicConsume(super.errorQueue(channel), true, consumer)

    errorQueueName
  }

  def jsonError(jsonError: JsonError): Any
}