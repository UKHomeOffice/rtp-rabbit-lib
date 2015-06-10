package uk.gov.homeoffice.rabbitmq

import akka.util.ByteString
import com.rabbitmq.client.Channel

class RabbitMessage(val deliveryTag: Long, val body: ByteString, channel: Channel) {
  def ack() = channel.basicAck(deliveryTag, false)

  def nack() = channel.basicNack(deliveryTag, false, true)
}

object RabbitMessage {
  case object OK

  case object KO
}