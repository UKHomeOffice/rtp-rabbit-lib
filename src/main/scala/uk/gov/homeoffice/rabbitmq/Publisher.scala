package uk.gov.homeoffice.rabbitmq

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import org.json4s.JValue
import org.json4s.JsonAST.JNothing
import org.json4s.jackson.JsonMethods._
import org.scalactic.{Bad, Good, Or}
import com.rabbitmq.client.{Channel, ConfirmListener, MessageProperties}
import grizzled.slf4j.Logging
import uk.gov.homeoffice.json.{JsonError, JsonValidator}

trait Publisher extends Logging {
 this: JsonValidator with Queue with Rabbit =>

 def publish(json: JValue): Future[JValue Or JsonError] = {
   val promise = Promise[JValue Or JsonError]()

   def ack = promise success Good(json)

   def nack = promise success Bad(JsonError(json, Some(s"Rabbit NACK - Failed to publish JSON ${if (json == JNothing) "" else pretty(render(json))}")))

   def onError(t: Throwable) = promise failure RabbitException(JsonError(json, Some("Failed to publish JSON"), Some(t)))

   validate(json) match {
     case Good(j) => publish(j, queue, ack, nack, onError)
     case Bad(e) => publishError(e).map(promise success Bad(_))
   }

   promise.future
 }

 def publishError(e: JsonError): Future[JsonError] = {
   publish(e, errorQueue)
 }

 def publishAlert(e: JsonError) = Future {
   publish(e, alertQueue)
 }

 private[rabbitmq] def publish(e: JsonError, queue: Channel => String): Future[JsonError] = {
   val promise = Promise[JsonError]()

   def ack = promise success e

   def nack = promise success e.copy(error = Some(s"Rabbit NACK - Failed to publish error JSON: ${e.error}"))

   def onError(t: Throwable) = promise failure RabbitException(e.copy(error = Some(s"Failed to publish error JSON${e.error.fold("")(e => ": " + e)}")))

   publish(e.asJson, queue, ack, nack, onError)

   promise.future
 }

 private[rabbitmq] def publish(json: JValue, queue: Channel => String, ack: => Any, nack: => Any, onError: Throwable => Any) = Future {
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
       error(t)
       onError(t)
   }
 }
}