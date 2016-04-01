package uk.gov.homeoffice.rabbitmq

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.actor.{ActorSystem, Props}
import org.json4s.JsonDSL._
import org.json4s.{DefaultFormats, JValue}
import uk.gov.homeoffice.configuration.HasConfig
import uk.gov.homeoffice.json.NoJsonValidator

/**
  * This example of booting an application to publish to RabbitMQ and consume, must have a local RabbitMQ running on default port of 5672.
  * If a ConfigFactory configuration such as application.conf is not provided (as in this case, but you should provide one), uk.gov.homeoffice.rabbitmq.Rabbit uses defaults.
  * To run, first start up RabbitMQ then:
  * sbt test:run
  */
object ExampleBoot extends App with HasConfig {
  implicit val json4sFormats = DefaultFormats

  val system = ActorSystem("rabbit-actor-system", config)

  // Consume
  system actorOf {
    Props {
      new ConsumerActor with Consumer[String] with RabbitErrorPolicy with NoJsonValidator with ExampleQueue with Rabbit {
        def consume(json: JValue) = Future {
          val message = (json \ "message").extract[String]
          println(s"Congratulations, consumed message '$message'")
          message
        }
      }
    }
  }

  // Publish
  val publisher = new Publisher with NoJsonValidator with ExampleQueue with Rabbit
  publisher.publish("message" -> "hello world!")
}

trait ExampleQueue extends Queue {
  def queueName = "rabbit-example"
}