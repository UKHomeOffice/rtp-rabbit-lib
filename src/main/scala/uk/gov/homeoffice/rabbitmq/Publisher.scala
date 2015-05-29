package uk.gov.homeoffice.rabbitmq

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import org.json4s.JValue
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
        case t: Throwable => promise failure new RabbitPublisherException(t, json)
      }
    }

    promise.future
  }
}