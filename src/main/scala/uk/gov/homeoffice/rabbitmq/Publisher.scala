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
    println(s"Going to publish json : ${json}")

    val promise = Promise[JValue Or JsonError]()

    Future {
      try {
        val channel = connection.createChannel()
        channel.confirmSelect()
        println(s"Rabbit mq channel $channel selected")
        channel.addConfirmListener(new ConfirmListener {
          def handleAck(deliveryTag: Long, multiple: Boolean) = {
            println(s"Handle acknowledgment for $deliveryTag")
            promise success Good(json)
          }

          def handleNack(deliveryTag: Long, multiple: Boolean) = {
            println(s"Handle non acknowledgment for $deliveryTag")
            promise success Bad(JsonError(json, s"Rabbit NACK - Failed to publish ${pretty(render(json))}"))
          }
        })

        println(s"Publishing to $connection") // TODO - Log as debug
        channel.basicPublish("", queue(channel), MessageProperties.PERSISTENT_BASIC, compact(render(json)).getBytes)
        print(s"Message has been published to the channel")
      } catch {
        
        case t: Throwable => {
          println(s"Unable to send message due to ERROR : ${t.printStackTrace()}")
          promise failure new RabbitPublisherException(t, json)}
      }
    }

    promise.future
  }
}