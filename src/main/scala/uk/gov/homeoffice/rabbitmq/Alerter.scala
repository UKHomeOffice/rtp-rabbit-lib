package uk.gov.homeoffice.rabbitmq

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import org.json4s.JsonAST.{JObject, JString}
import org.json4s._
import org.json4s.native.JsonMethods._
import com.rabbitmq.client.{Channel, ConfirmListener, MessageProperties}
import grizzled.slf4j.Logging
import uk.gov.homeoffice.json.JsonError

trait Alerter extends Logging {
  this: Rabbit =>

  val alertQueueName = "alert"

  def alertQueue(channel: Channel): String =
    channel.queueDeclare(alertQueueName, /*durable*/true, /*exclusive*/false, /*autoDelete*/false, /*arguments*/Map("x-ha-policy" -> "all")).getQueue

  def alert(e: JsonError) = Future {
    val promise = Promise[JsonError]()

    try {
      val channel = connection.createChannel()
      channel.confirmSelect()

      channel.addConfirmListener(new ConfirmListener {
        def handleAck(deliveryTag: Long, multiple: Boolean) = promise success e

        def handleNack(deliveryTag: Long, multiple: Boolean) = promise success e.copy(error = s"Rabbit NACK - Failed to publish error JSON: ${e.error}")
      })

      val jsonWithError: JValue = e.json merge JObject("error" -> JString(e.error))
      info(s"Publishing to $connection:${alertQueue(channel)} ${pretty(render(jsonWithError))}")
      channel.basicPublish("", alertQueue(channel), MessageProperties.PERSISTENT_BASIC, compact(render(jsonWithError)).getBytes)

      promise.future
    } catch {
      case t: Throwable =>
        logger.error(t)
        promise failure RabbitException(e.copy(error = s"Failed to publish error JSON: ${e.error}"))
    }
  }
}
