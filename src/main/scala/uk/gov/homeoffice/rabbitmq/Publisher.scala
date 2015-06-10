package uk.gov.homeoffice.rabbitmq

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import org.json4s.JValue
import org.json4s.native.JsonMethods._
import org.scalactic.{ErrorMessage, Bad, Good, Or}
import com.rabbitmq.client.{Channel, ConfirmListener, MessageProperties}
import uk.gov.homeoffice.json.JsonError

trait Publisher {
  this: Queue with Rabbit =>
  
  def publish(json: JValue): Future[JValue Or JsonError] = {
    val promise = Promise[JValue Or JsonError]()

    def ack = promise success Good(json)

    def nack = promise success Bad(JsonError(json, s"Rabbit NACK - Failed to publish JSON ${pretty(render(json))}"))

    def error(t: Throwable) = promise failure RabbitException(t, "Failed to publish JSON", Some(json))

    println(s"Publishing JSON to $connection") // TODO - Log as debug
    publish(compact(render(json)).getBytes, queue, ack, nack, error)

    promise.future
  }

  def publishError(json: JValue): Future[JValue Or JsonError] = {
    val promise = Promise[JValue Or JsonError]()

    def ack = promise success Good(json)

    def nack = promise success Bad(JsonError(json, s"Rabbit NACK - Failed to publish error JSON ${pretty(render(json))}"))

    def error(t: Throwable) = promise failure RabbitException(t, "Failed to publish error JSON", Some(json))

    println(s"Publishing error JSON to $connection") // TODO - Log as debug
    publish(compact(render(json)).getBytes, errorQueue, ack, nack, error)

    promise.future
  }

  def publishError(data: Array[Byte]): Future[Array[Byte] Or ErrorMessage] = {
    val promise = Promise[Array[Byte] Or ErrorMessage]()

    def ack = promise success Good(data)

    def nack = promise success Bad(s"Rabbit NACK - Failed to publish error ${data.mkString}")

    def error(t: Throwable) = promise failure RabbitException(t, "Failed to publish error", Some(data.mkString))

    println(s"Publishing error to $connection") // TODO - Log as debug
    publish(data, errorQueue, ack, nack, error)

    promise.future
  }

  private[rabbitmq] def publish(data: Array[Byte], queue: Channel => String, ack: => Any, nack: => Any, error: Throwable => Any) = Future {
    try {
      val channel = connection.createChannel()
      channel.confirmSelect()

      channel.addConfirmListener(new ConfirmListener {
        def handleAck(deliveryTag: Long, multiple: Boolean) = ack

        def handleNack(deliveryTag: Long, multiple: Boolean) = nack
      })

      channel.basicPublish("", queue(channel), MessageProperties.PERSISTENT_BASIC, data)
    } catch {
      case t: Throwable => error(t)
    }
  }
}