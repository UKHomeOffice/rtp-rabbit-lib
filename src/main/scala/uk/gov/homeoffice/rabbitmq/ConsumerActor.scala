package uk.gov.homeoffice.rabbitmq

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import akka.util.ByteString
import org.json4s._
import org.json4s.native.JsonMethods._
import org.scalactic.{Bad, Good, Or}
import com.rabbitmq.client._
import uk.gov.homeoffice.json.{JsonError, JsonValidator}
import uk.gov.homeoffice.rabbitmq.RabbitMessage.{KO, OK}

trait ConsumerActor extends Actor with ActorLogging with Publisher {
  this: Consumer[_] with JsonValidator with Queue with Rabbit =>

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
    try channel.close() catch { case t: Throwable => log.error(t.getMessage) }
  }

  // TODO This function needs another iteration as I'm not happy with this first attempt of the implementation!!!
  private[rabbitmq] def consume(rabbitMessage: RabbitMessage, sender: ActorRef): Unit = {
    val jsonError: PartialFunction[_ Or JsonError, _ Or JsonError] = {
      case b @ Bad(e @ JsonError(_, _, _, fatalException)) =>
        if (fatalException) {
          log.error(s"NACKing fatal exception while processing: $e")
          rabbitMessage.nack()
        } else {
          log.error(s"BAD processing: $e")
          publish(e)
          rabbitMessage.ack()
        }

        sender ! KO
        b
    }

    val validateJson: PartialFunction[JValue Or JsonError, JValue Or JsonError] = {
      case Good(json) => validate(json)
    }

    val consumeJson: PartialFunction[JValue Or JsonError, Unit] = {
      case Good(json) =>
        consume(json).collect {
          case Good(_) =>
            log.debug("GOOD processing")
            rabbitMessage.ack()
            sender ! OK

          case b @ Bad(_) =>
            jsonError(b)
        }

      case b @ Bad(_) =>
        jsonError(b)
    }

    (jsonError orElse (validateJson andThen consumeJson))(parseBody(rabbitMessage.body.utf8String))
  }

  private[rabbitmq] def parseBody(body: String): JValue Or JsonError = Try {
    new Good(parse(body))
  } getOrElse new Bad(JsonError(json = JObject("data" -> JString(body)), error = "Invalid JSON format"))
}