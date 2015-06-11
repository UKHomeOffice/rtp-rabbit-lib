package uk.gov.homeoffice.rabbitmq

import java.util.UUID
import scala.collection.JavaConversions._
import com.rabbitmq.client.Channel

/**
 * Not nice to name a trait prefixed by "With" as it will probably mixed in using "with".
 * However, this seems to be a naming idiom (certainly from Play) to distinguish this trait that is only for testing as opposed to say main code named "Queue"
 */
trait WithQueue extends Queue {
  override val queueName: String = UUID.randomUUID().toString

  override def queue(channel: Channel): String =
    channel.queueDeclare(queueName, /*durable*/false, /*exclusive*/true, /*autoDelete*/true, /*arguments*/Map("passive" -> "false")).getQueue

  override def errorQueue(channel: Channel): String =
    channel.queueDeclare(errorQueueName, /*durable*/false, /*exclusive*/true, /*autoDelete*/true, /*arguments*/Map("passive" -> "false")).getQueue
}