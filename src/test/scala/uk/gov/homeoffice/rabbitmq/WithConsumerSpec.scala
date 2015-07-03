package uk.gov.homeoffice.rabbitmq

import scala.concurrent.Promise
import scala.concurrent.duration._
import org.json4s.JsonAST.JObject
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import uk.gov.homeoffice.json.JsonError

class WithConsumerSpec(implicit ev: ExecutionEnv) extends Specification with RabbitSpecification {
  "Consumer" should {
    "consume valid and error message" in {
      val validMessageConsumed = Promise[Boolean]()
      val errorMessageConsumed = Promise[Boolean]()

      val publisher = new Publisher with WithConsumer with WithErrorConsumer with WithQueue with WithRabbit {
        def consume(body: Array[Byte]) = validMessageConsumed success true

        def consumeError(body: Array[Byte]) = errorMessageConsumed success true
      }

      publisher.publish(JsonError(JObject(), "Error"))
      publisher.publish(JObject())

      validMessageConsumed.future must beTrue.awaitFor(10 seconds)
      errorMessageConsumed.future must beTrue.awaitFor(10 seconds)
    }

    "consume valid message" in {
      val validMessageConsumed = Promise[Boolean]()

      val publisher = new Publisher with WithConsumer with WithQueue with WithRabbit {
        def consume(body: Array[Byte]) = validMessageConsumed success true
      }

      publisher.publish(JObject())

      validMessageConsumed.future must beTrue.awaitFor(10 seconds)
    }

    "consume error message" in {
      val errorMessageConsumed = Promise[Boolean]()

      val publisher = new Publisher with WithConsumer with WithErrorConsumer with WithQueue with WithRabbit {
        def consume(body: Array[Byte]) = ko

        def consumeError(body: Array[Byte]) = errorMessageConsumed success true
      }

      publisher.publish(JsonError(JObject(), "Error"))

      errorMessageConsumed.future must beTrue.awaitFor(10 seconds)
    }
  }
}