package uk.gov.homeoffice.rabbitmq

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import org.json4s.JValue
import org.json4s.JsonAST.{JObject, JString}
import org.json4s.native.JsonMethods._
import org.scalactic.{Bad, Good, Or}
import com.rabbitmq.client.{Channel, ConfirmListener, MessageProperties}
import grizzled.slf4j.Logging
import uk.gov.homeoffice.json.{JsonError, JsonValidator}

trait Publisher extends Logging {
  this: JsonValidator with Queue with Rabbit =>

  def publish(json: JValue): Future[JValue Or JsonError] = {
    val promise = Promise[JValue Or JsonError]()

    def ack = promise success Good(json)

    def nack = promise success Bad(JsonError(json, s"Rabbit NACK - Failed to publish JSON ${pretty(render(json))}"))

    def error(t: Throwable) = promise failure RabbitException(JsonError(json, "Failed to publish JSON", Some(t)))

    validate(json) match {
      case Good(j) => publish(j, queue, ack, nack, error)
      case Bad(e) => publish(e).map(promise success Bad(_))
    }

    promise.future
  }

  def publish(e: JsonError): Future[JsonError] = {
    val promise = Promise[JsonError]()

    def ack = promise success e

    def nack = promise success e.copy(error = s"Rabbit NACK - Failed to publish error JSON: ${e.error}")

    def error(t: Throwable) = promise failure RabbitException(e.copy(error = s"Failed to publish error JSON: ${e.error}"))

    val jsonWithError: JValue = e.json merge JObject("error" -> JString(e.error))

    info(s"Publishing to error queue $errorQueueName ${pretty(render(jsonWithError))}")
    publish(jsonWithError, errorQueue, ack, nack, error)

    promise.future
  }

  def alert(e: JsonError) = Future {
    val promise = Promise[JsonError]()

    def ack = promise success e

    def nack = promise success e.copy(error = s"Rabbit NACK - Failed to publish error JSON: ${e.error}")

    def error(t: Throwable) = promise failure RabbitException(e.copy(error = s"Failed to publish error JSON: ${e.error}"))

    val jsonWithError: JValue = e.json merge JObject("error" -> JString(e.error))

    info(s"Publishing to alert queue $alertQueueName ${pretty(render(jsonWithError))}")
    publish(jsonWithError, alertQueue, ack, nack, error)

    promise.future
  }

  private[rabbitmq] def publish(json: JValue, queue: Channel => String, ack: => Any, nack: => Any, err: Throwable => Any) = Future {
    try {
      val channel = connection.createChannel()
      channel.confirmSelect()

      channel.addConfirmListener(new ConfirmListener {
        def handleAck(deliveryTag: Long, multiple: Boolean) = ack

        def handleNack(deliveryTag: Long, multiple: Boolean) = nack
      })

      info(s"Publishing to $connection:${queue(channel)} ${pretty(render(json))}")
      channel.basicPublish("", queue(channel), MessageProperties.PERSISTENT_BASIC, compact(render(json)).getBytes)
    } catch {
      case t: Throwable =>
        logger.error(t)
        err(t)
    }
  }
}