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
import uk.gov.homeoffice.akka.ActorHasConfig
import uk.gov.homeoffice.configuration.ConfigFactorySupport
import uk.gov.homeoffice.json.{JsonError, JsonValidator}
import uk.gov.homeoffice.rabbitmq.RabbitMessage.{KO, OK}
import uk.gov.homeoffice.rabbitmq.RetryStrategy.{ExceededMaximumRetries, Ok}

trait ConsumerActor extends Actor with ActorLogging with ActorHasConfig with ConfigFactorySupport with Publisher {
  this: Consumer[_] with JsonValidator with Queue with Rabbit =>

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
          Thread.sleep(retryStrategy.delay.toMillis)
          consume(m, sender())

        case ExceededMaximumRetries =>
          // TODO STOP
          println("===> Stopping Actor because retries have been exceeded")
          context.stop(self)
      }
  }

  override def postStop() = {
    super.postStop()
    try channel.basicCancel(consumer.getConsumerTag) catch { case t: Throwable => log.error(t.getMessage) }
    try channel.close() catch { case t: Throwable => log.error(t.getMessage) }
  }

  private[rabbitmq] def consume(rabbitMessage: RabbitMessage, sender: ActorRef): Future[_ Or JsonError] = {
    def goodConsume() = {
      log.info("GOOD processing - ACKing")
      context.become(receive)
      rabbitMessage.ack()
      sender ! OK
    }

    def badConsume(jsonError: JsonError) = {
      jsonError match {
        case e @ JsonError(_, _, Some(AlertThrowable(t))) =>
          log.error(s"ALERT BAD processing: $e")
          publishAlert(e)
          context.become(receive)
          rabbitMessage.ack()

        case e @ JsonError(_, _, Some(RetryThrowable(t))) =>
          log.error(s"Prepare for retry - NACKing exception while processing: $e")
          context.become(retry)
          rabbitMessage.nack()

        case e: JsonError =>
          log.error(s"Publishing to error queue because of BAD processing: $e")
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
  } getOrElse new Bad(JsonError(json = JObject("data" -> JString(body)), error = "Invalid JSON format"))
}