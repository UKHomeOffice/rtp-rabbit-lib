package uk.gov.homeoffice.rabbitmq

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import akka.util.ByteString
import com.rabbitmq.client._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.scalactic.{Bad, Good, Or}
import uk.gov.homeoffice.akka.ActorHasConfig
import uk.gov.homeoffice.configuration.ConfigFactorySupport
import uk.gov.homeoffice.json.{JsonError, JsonValidator}
import uk.gov.homeoffice.rabbitmq.RabbitMessage.{KO, OK}
import uk.gov.homeoffice.rabbitmq.RetryStrategy.{ExceededMaximumRetries, Ok}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

/**
 * Mixin a Consumer for this Actor to delegate messages to.
 * Messages will be consumed by this Actor from a given Rabbit Queue, where a message (bytes) will be converted to JSON (JValue).
 * The JSON, now representing a Rabbit message, will be ACKed, or NACKed when a Consumer has finished its job, where said job is wrapped in a Future,
 * so the acking actually takes place when the Future is completed.
 * To use this Actor (as well as mixing in Rabbit and associated Queue) a JsonValidator and ErrorPolicy must be mixed in:
 * JsonValidator is used validate the message against a JSON schema, which your JsonValidator should use, but the validation is up to said validator.
 * ErrorPolicy dicates how errors are handled, specifically how a JsonError is handled.
 * An error message could be retried according to a RetryStrategy, or placed onto an error or alert queue.
 */
trait ConsumerActor extends Actor with ActorHasConfig with ConfigFactorySupport with Publisher {
  this: Consumer[_] with ErrorPolicy with JsonValidator with Queue with Rabbit =>

  lazy val channel = connection.createChannel()

  lazy val consumer = new DefaultConsumer(channel) {
    override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]) =
      self ! new RabbitMessage(envelope.getDeliveryTag, ByteString(body), channel)
  }

  val retryStrategy = new RetryStrategy()

  override def preStart() = {
    super.preStart()
    channel.basicConsume(queue(channel), false, consumer)
  }

  override def receive = LoggingReceive {
    case m: RabbitMessage =>
      retryStrategy.reset()
      consume(m, sender())
  }

  def retry = LoggingReceive {
    case m: RabbitMessage =>
      retryStrategy.increment match {
        case Ok =>
          context.system.scheduler.scheduleOnce(retryStrategy.delay) {
            consume(m, sender())
          }

        case ExceededMaximumRetries =>
          warn("Stopping Actor because retries have been exceeded")
          context.stop(self)
      }
  }

  override def postStop() = {
    super.postStop()
    try channel.basicCancel(consumer.getConsumerTag) catch { case t: Throwable => error(t.getMessage) }
    try channel.close() catch { case t: Throwable => error(t.getMessage) }
  }

  private[rabbitmq] def consume(rabbitMessage: RabbitMessage, sender: ActorRef): Future[_ Or JsonError] = {
    def goodConsume() = {
      info("GOOD processing - ACKing")
      context.become(receive)
      rabbitMessage.ack()
      sender ! OK
    }

    def badConsume(jsonError: JsonError) = {
      enforce(jsonError) match {
        case e: JsonError with Alert =>
          error(s"ALERT BAD processing: $e")
          publishAlert(e)
          context.become(receive)
          rabbitMessage.ack()

        case e: JsonError with Retry =>
          error(s"Prepare for retry - NACKing exception while processing: $e")
          context.become(retry)
          rabbitMessage.nack()

        case e: JsonError =>
          error(s"Publishing to error queue because of BAD processing: $e")
          publishError(e)
          context.become(receive)
          rabbitMessage.ack()
      }

      sender ! KO
    }

    val result = for {
      json <- parseBody(rabbitMessage.body.utf8String)
      validJson <- validate(json)
    } yield validJson

    result match {
      case Good(json) =>
        consume(json).collect {
          case g @ Good(_) =>
            goodConsume()
            g

          case b @ Bad(jsonError) =>
            badConsume(jsonError)
            b
        }

      case b @ Bad(jsonError: JsonError) =>
        badConsume(jsonError)
        Future.successful(b)
    }
  }

  private[rabbitmq] def parseBody(body: String): JValue Or JsonError = Try {
    new Good(parse(body))
  } getOrElse new Bad(JsonError(json = JObject("data" -> JString(body)), error = Some("Invalid JSON format")))
}