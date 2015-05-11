package uk.gov.homeoffice.rabbitmq

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.actor.{ActorSystem, Props}
import org.json4s.JsonAST.{JObject, JString}
import org.json4s.{DefaultFormats, JValue}
import org.scalautils.Good
import uk.gov.homeoffice.HasConfig

/**
 * This example of booting an application to publish to RabbitMQ and consume, must have a local RabbitMQ running on default port of 5672.
 * The configuration can be found in the default application.conf
 */
object ExampleBoot extends App with HasConfig {
  implicit val json4sFormats = DefaultFormats

  val system = ActorSystem("importer-actor-system", config)

  // Consume
  system.actorOf(Props(new ConsumerActor with Consumer[String] with ExampleQueue with Rabbit {
    def consume(json: JValue) = Future {
      val message = (json \ "message").extract[String]
      println(s"Congratulations, consumed message '$message'")
      Good(message)
    }
  }))

  // Publish
  val publisher = new Publisher with ExampleQueue with Rabbit
  publisher.publish(JObject("message" -> JString("hello world!")))
}

trait ExampleQueue extends Queue {
  def queueName = "rabb-it-example"
}