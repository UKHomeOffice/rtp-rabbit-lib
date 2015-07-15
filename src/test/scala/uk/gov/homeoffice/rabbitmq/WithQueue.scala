package uk.gov.homeoffice.rabbitmq

import java.util.UUID
import scala.collection.JavaConversions._
import org.json4s._
import org.json4s.native.JsonMethods._
import com.rabbitmq.client.{AMQP, Channel, DefaultConsumer, Envelope}
import uk.gov.homeoffice.json.{JsonFormats, JsonError}

/**
 * Not nice to name a trait prefixed by "With" as it will probably mixed in using "with".
 * However, this seems to be a naming idiom (certainly from Play) to distinguish this trait that is only for testing as opposed to say main code named "Queue"
 */
trait WithQueue extends Queue with JsonFormats {
  override val queueName = UUID.randomUUID().toString

  override val alertQueueName = s"$queueName-alert"

  override def queue(channel: Channel): String =
    channel.queueDeclare(queueName, /*durable*/ false, /*exclusive*/ true, /*autoDelete*/ true, /*arguments*/ Map("passive" -> "false")).getQueue

  override def errorQueue(channel: Channel): String =
    channel.queueDeclare(errorQueueName, /*durable*/ false, /*exclusive*/ true, /*autoDelete*/ true, /*arguments*/ Map("passive" -> "false")).getQueue

  override def alertQueue(channel: Channel): String =
    channel.queueDeclare(alertQueueName, /*durable*/ false, /*exclusive*/ true, /*autoDelete*/ true, /*arguments*/ Map("passive" -> "false")).getQueue
}

object WithQueue {
  trait Consumer extends WithQueue {
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

  trait ErrorConsumer extends WithQueue {
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

  trait AlertConsumer extends WithQueue {
    override def alertQueue(channel: Channel): String = {
      val consumer = new DefaultConsumer(channel) {
        override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]) = {
          super.handleDelivery(consumerTag, envelope, properties, body)

          alertError {
            val extractedJsonError = parse(new String(body))
            JsonError(extractedJsonError, error = (extractedJsonError \ "error").extract[String])
          }
        }
      }

      channel.basicConsume(super.alertQueue(channel), true, consumer)

      alertQueueName
    }

    def alertError(jsonError: JsonError): Any
  }
}