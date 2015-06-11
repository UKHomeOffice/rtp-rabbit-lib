package uk.gov.homeoffice.rabbitmq

import org.json4s.JsonAST.JObject
import org.specs2.mutable.Specification
import uk.gov.homeoffice.json.JsonError

class WithConsumerSpec extends Specification with RabbitSpec {
  // TODO DOES NOT RUN ON JENKINS ???
  /*"Message" should {
    "be consumed from error queue" in {
      val publisher = new Publisher with WithConsumer with WithQueue with WithRabbit {
        def consume(body: Array[Byte]) = println("Consumed")

        override def consumeError(body: Array[Byte]) = println("Consumed Error")
      }

      publisher.publish(JsonError(JObject(), "Error"))
      publisher.publish(JObject())

      Thread.sleep(2000)
      ok
    }
  }*/
}