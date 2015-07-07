package uk.gov.homeoffice.rabbitmq

import org.json4s.JValue
import org.json4s.native.JsonMethods._
import com.rabbitmq.client._

trait JsonConsumer extends WithQueue {
  this: WithQueue =>

  override def queue(channel: Channel): String = {
    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]) = {
        super.handleDelivery(consumerTag, envelope, properties, body)
        json(parse(new String(body)))
      }
    }

    channel.basicConsume(super.queue(channel), true, consumer)

    queueName
  }

  def json(json: JValue): Any
}