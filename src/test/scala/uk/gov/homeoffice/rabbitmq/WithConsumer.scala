package uk.gov.homeoffice.rabbitmq

import com.rabbitmq.client._

/**
 * Not nice to name a trait prefixed by "With" as it will probably mixed in using "with".
 * However, this seems to be a naming idiom (certainly from Play) to distinguish this trait that is only for testing as opposed to say main code named "Consumer"
 */
trait WithConsumer extends WithQueue {
  this: WithQueue =>

  override def queue(channel: Channel): String = {
    def consumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]) = {
        super.handleDelivery(consumerTag, envelope, properties, body)
        consume(body) // Note - Not passing in "consumerTag", "envelope" and "properties" as seems unnecessary and avoids AMQP dependency for client code.
      }
    }

    val queueName = super.queue(channel)

    channel.basicConsume(queueName, true, consumer)
    channel.basicConsume(s"$queueName-error", true, consumer)

    queueName
  }

  def consume(body: Array[Byte]): Any

  def consumeError(body: Array[Byte]): Any = ()
}