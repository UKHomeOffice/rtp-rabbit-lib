package uk.gov.homeoffice.rabbitmq

import scala.concurrent.Future
import org.json4s.JValue
import org.scalactic.Bad
import com.rabbitmq.client._
import uk.gov.homeoffice.json.JsonError

/**
 * Not nice to name a trait prefixed by "With" as it will probably mixed in using "with".
 * However, this seems to be a naming idiom (certainly from Play) to distinguish this trait that is only for testing as opposed to say main code named "Consumer"
 */
trait WithConsumer extends Consumer[Any] with WithQueue {
  this: WithQueue =>

  override def queue(channel: Channel): String = {
    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]) = {
        super.handleDelivery(consumerTag, envelope, properties, body)
        consume(body) // Note - Not passing in "consumerTag", "envelope" and "properties" as seems unnecessary and avoids AMQP dependency for client code.
      }
    }

    channel.basicConsume(super.queue(channel), true, consumer)

    queueName
  }

  def consume(json: JValue) = Future.successful(Bad(JsonError(error = "Consumed by WithConsumer - you should override this is used as part of your test to be explicit")))

  def consume(body: Array[Byte]): Any
}

trait WithErrorConsumer extends WithQueue {
  this: WithConsumer =>

  override def errorQueue(channel: Channel): String = {
    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]) = {
        super.handleDelivery(consumerTag, envelope, properties, body)
        consumeError(body) // Note - Not passing in "consumerTag", "envelope" and "properties" as seems unnecessary and avoids AMQP dependency for client code.
      }
    }

    channel.basicConsume(super.errorQueue(channel), true, consumer)

    errorQueueName
  }

  override def consume(body: Array[Byte]) = ()

  def consumeError(body: Array[Byte]): Any
}