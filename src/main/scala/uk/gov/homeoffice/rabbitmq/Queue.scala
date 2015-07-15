package uk.gov.homeoffice.rabbitmq

import scala.collection.JavaConversions._
import com.rabbitmq.client.Channel

trait Queue {
  def queueName: String

  lazy val errorQueueName: String = s"$queueName-error"

  val alertQueueName = "alert"

  /**
   * x-ha-policy tells RabbitMQ to make this a queue that is mirrored across all nodes
   */
  def queue(channel: Channel): String =
    channel.queueDeclare(queueName, /*durable*/true, /*exclusive*/false, /*autoDelete*/false, /*arguments*/Map("x-ha-policy" -> "all")).getQueue

  def errorQueue(channel: Channel): String =
    channel.queueDeclare(errorQueueName, /*durable*/true, /*exclusive*/false, /*autoDelete*/false, /*arguments*/Map("x-ha-policy" -> "all")).getQueue

  def alertQueue(channel: Channel): String =
    channel.queueDeclare(alertQueueName, /*durable*/true, /*exclusive*/false, /*autoDelete*/false, /*arguments*/Map("x-ha-policy" -> "all")).getQueue
}