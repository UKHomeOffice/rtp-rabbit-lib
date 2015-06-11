package uk.gov.homeoffice.rabbitmq

import com.rabbitmq.client.{Channel, ConfirmListener, MessageProperties}
import org.json4s.JValue
import org.json4s.native.JsonMethods._
import org.scalactic.{Bad, Good, Or}
import uk.gov.homeoffice.json.JsonError

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

trait Publisher {
  this: Queue with Rabbit =>

  def publish(json: JValue): Future[JValue Or JsonError] = {

    val promise = Promise[JValue Or JsonError]()

    def ack = promise success Good(json)

    def nack = promise success Bad(JsonError(json, s"Rabbit NACK - Failed to publish JSON ${pretty(render(json))}"))

    def error(t: Throwable) = promise failure new RabbitException(JsonError(json, "Failed to publish JSON", Some(t)))

    println(s"Publishing JSON to $connection") // TODO - Log as debug
    publish(json, queue, ack, nack, error)

    promise.future
  }


  private[rabbitmq] def publish(json: JValue, queue: Channel => String, ack: => Any, nack: => Any, error: Throwable => Any) = Future {
    try {

      println(s"Going to publish json : ${json}")

      val channel = connection.createChannel()
      channel.confirmSelect()
      println(s"Rabbit mq channel $channel selected")
      channel.addConfirmListener(new ConfirmListener {
        def handleAck(deliveryTag: Long, multiple: Boolean) = ack

        def handleNack(deliveryTag: Long, multiple: Boolean) = nack
      })

      channel.basicPublish("", queue(channel), MessageProperties.PERSISTENT_BASIC, compact(render(json)).getBytes)
    } catch {
      case t: Throwable => {
        println(s"Unable to send message due to ERROR : ${t.printStackTrace()}")
        error(t)
      }
    }
  }
}