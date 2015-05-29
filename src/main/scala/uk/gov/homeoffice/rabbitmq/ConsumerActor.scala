package uk.gov.homeoffice.rabbitmq

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import akka.util.ByteString
import org.json4s._
import org.json4s.native.JsonMethods._
import org.scalactic.{Bad, Good, Or}
import com.rabbitmq.client._
import uk.gov.homeoffice.json.JsonError

trait ConsumerActor extends Actor with ActorLogging {
  this: Consumer[_] with Queue with Rabbit =>

  lazy val channel = connection.createChannel()

  override def preStart() = {
    super.preStart()

    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]) = {
        self ! new RabbitMessage(envelope.getDeliveryTag, ByteString(body), channel)
      }
    }

    channel.basicConsume(queue(channel), false, consumer)
  }

  override def receive = LoggingReceive {
    case m: RabbitMessage => consume(m, sender())
  }

  override def postStop() = {
    super.postStop()
    try channel.close() catch { case t: Throwable => }
  }

  private def consume(rabbitMessage: RabbitMessage, sender: ActorRef): Unit = Try {
    val result = parseBody(rabbitMessage.body.utf8String).fold(validJson => consume(validJson),
      invalidJson => Future.successful(new Bad(invalidJson)))
    result collect {
      case Good(_) =>
        log.debug("GOOD processing")
        rabbitMessage.ack()
        sender ! "ok"

      case Bad(j @ JsonError(_, _, _, fatalException)) =>
        if (fatalException) {
          log.error(s"NACKing fatal exception while processing: $j")
          rabbitMessage.nack()
        } else {
          log.error(s"BAD processing: $j")
          publishError(compact(render(j.json merge JObject("error" -> JString(j.error)))).getBytes)
          rabbitMessage.ack()
        }

        sender ! "ko"
    }
  } getOrElse {
    val unknown = rabbitMessage.body.utf8String
    log.error(s"UKNOWN MESSAGE TYPE WITH CONTENT: $unknown")
    publishError(compact(render(JObject("data" -> JString(unknown)) merge JObject("error" -> JString("Unknown data")))).getBytes)
    rabbitMessage.ack()
    sender ! "ko"
  }

  def parseBody(body: String): JValue Or JsonError = Try {
    new Good(parse(body))
  } getOrElse new Bad(JsonError(json = JObject("data" -> JString(body)), error = "Invalid JSON format"))

  private def publishError(data: Array[Byte]) = channel.basicPublish("", errorQueue(channel), MessageProperties.PERSISTENT_BASIC, data)
}