package uk.gov.homeoffice.rabbitmq

import scala.concurrent.duration._
import scala.concurrent.Promise
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import uk.gov.homeoffice.json.{NoJsonValidator, JsonValidator, JsonSchema}

class PublisherSpec(implicit ev: ExecutionEnv) extends Specification with RabbitSpecification {
  trait TestJsonValidator extends JsonValidator {
    override val jsonSchema = JsonSchema(parse("""
    {
      "$schema": "http://json-schema.org/draft-04/schema#",
      "id": "http://www.gov.uk/unique-schema-id",
      "type": "object",
      "properties": {
        "valid": {
          "type": "string"
        },
        "valid-again": {
          "type": "string"
        }
      },
      "additionalProperties": false,
      "required": [
        "valid",
        "valid-again"
      ]
    }"""))
  }

  "JSON" should {
    "be published to Rabbit queue when it conforms to a given schema" in {
      val validMessageConsumed = Promise[Boolean]()

      val publisher = new Publisher with TestJsonValidator with WithConsumer with WithQueue with WithRabbit {
        def consume(body: Array[Byte]) = validMessageConsumed success true
      }

      publisher.publish(("valid" -> "json") ~ ("valid-again" -> "json"))

      validMessageConsumed.future must beTrue.awaitFor(10 seconds)
    }

    "be published to Rabbit error queue when it does not conform to a given schema because of an extra field" in {
      val errorMessageConsumed = Promise[Boolean]()

      val publisher = new Publisher with TestJsonValidator with WithErrorConsumer with WithQueue with WithRabbit {
        def consumeError(body: Array[Byte]) = {
          println(pretty(render(parse(new String(body)))))
          errorMessageConsumed success true
        }
      }

      publisher.publish(("valid" -> "json") ~ ("valid-again" -> "json") ~ ("invalid" -> "json"))

      errorMessageConsumed.future must beTrue.awaitFor(10 seconds)
    }

    "be published to Rabbit error queue when it does not conform to a given schema because of a missing field" in {
      val errorMessageConsumed = Promise[Boolean]()

      val publisher = new Publisher with TestJsonValidator with WithErrorConsumer with WithQueue with WithRabbit {
        def consumeError(body: Array[Byte]) = {
          println(pretty(render(parse(new String(body)))))
          errorMessageConsumed success true
        }
      }

      publisher.publish("valid" -> "json")

      errorMessageConsumed.future must beTrue.awaitFor(10 seconds)
    }

    "not be validated when no JSON Validator is provided and so just publish onto Rabbit queue" in {
      val messageConsumed = Promise[Boolean]()

      val publisher = new Publisher with NoJsonValidator with WithConsumer with WithQueue with WithRabbit {
        def consume(body: Array[Byte]) = messageConsumed success true
      }

      publisher.publish("whatever" -> "json")

      messageConsumed.future must beTrue.awaitFor(10 seconds)
    }
  }
}