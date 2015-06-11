package uk.gov.homeoffice.rabbitmq

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import org.json4s.JValue
import org.json4s.JsonAST.{JObject, JString}
import org.json4s.native.JsonMethods._
import org.scalactic.{Bad, Good, Or}
import com.rabbitmq.client.{ConfirmListener, MessageProperties}
import uk.gov.homeoffice.json.JsonError

trait Publisher {
  this: Queue with Rabbit =>

  def publish(json: JValue): Future[JValue Or JsonError] = {
    val promise = Promise[JValue Or JsonError]()

    Future {
      try {
        val channel = connection.createChannel()
        channel.confirmSelect()

        channel.addConfirmListener(new ConfirmListener {
          def handleAck(deliveryTag: Long, multiple: Boolean) = {
            promise success Good(json)
          }

          def handleNack(deliveryTag: Long, multiple: Boolean) = {
            promise success Bad(JsonError(json, s"Rabbit NACK - Failed to publish ${pretty(render(json))}"))
          }
        })

        println(s"Publishing to $connection") // TODO - Log as debug
        channel.basicPublish("", queue(channel), MessageProperties.PERSISTENT_BASIC, compact(render(json)).getBytes)
      } catch {
        case t: Throwable => promise failure new RabbitException(JsonError(json, "Failed to publish JSON", Some(t)))
      }
    }

    promise.future
  }

  def publish(e: JsonError): Future[JsonError] = {
    val promise = Promise[JsonError]()

    Future {
      try {
        val channel = connection.createChannel()
        channel.confirmSelect()

        channel.addConfirmListener(new ConfirmListener {
          def handleAck(deliveryTag: Long, multiple: Boolean) = promise success e

          def handleNack(deliveryTag: Long, multiple: Boolean) = {
            promise success e.copy(error = s"Rabbit NACK - Failed to publish error JSON: ${e.error}")
          }
        })

        println(s"Publishing error to $connection") // TODO - Log as debug
        channel.basicPublish("", errorQueue(channel), MessageProperties.PERSISTENT_BASIC, compact(render(e.json merge JObject("error" -> JString(e.error)))).getBytes)
      } catch {
        case t: Throwable =>
          t.printStackTrace()
          promise failure new RabbitException(e.copy(error = s"Failed to publish error JSON: ${e.error}"))
      }
    }

    promise.future
  }
}