package uk.gov.homeoffice.rabbitmq

import scala.concurrent.Promise
import scala.concurrent.duration._
import org.json4s.JValue
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import uk.gov.homeoffice.json.{JsonError, JsonSchema, NoJsonValidator}

class PublisherSpec(implicit ev: ExecutionEnv) extends Specification with RabbitSpecification {
  trait JsonValidator extends uk.gov.homeoffice.json.JsonValidator {
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
      val jsonPromise = Promise[JValue]()

      val publisher = new Publisher with JsonValidator with WithQueue.Consumer with WithRabbit {
        def json(json: JValue) = jsonPromise success json
      }

      publisher.publish(("valid" -> "json") ~ ("valid-again" -> "json"))

      jsonPromise.future must beLike[JValue] {
        case _: JValue => ok
      }.awaitFor(10 seconds)
    }

    "be published to Rabbit error queue when it does not conform to a given schema because of an extra field" in {
      val jsonErrorPromise = Promise[JsonError]()

      val publisher = new Publisher with JsonValidator with WithQueue.ErrorConsumer with WithRabbit {
        def jsonError(jsonError: JsonError) = jsonErrorPromise success jsonError
      }

      publisher.publish(("valid" -> "json") ~ ("valid-again" -> "json") ~ ("invalid" -> "json"))

      jsonErrorPromise.future must beLike[JsonError] {
        case JsonError(_, error, _) => ok
      }.awaitFor(10 seconds)
    }

    "be published to Rabbit error queue when it does not conform to a given schema because of a missing field" in {
      val jsonErrorPromise = Promise[JsonError]()

      val publisher = new Publisher with JsonValidator with WithQueue.ErrorConsumer with WithRabbit {
        def jsonError(jsonError: JsonError) = jsonErrorPromise success jsonError
      }

      publisher.publish("valid" -> "json")

      jsonErrorPromise.future must beLike[JsonError] {
        case JsonError(_, error, _) => ok
      }.awaitFor(10 seconds)
    }

    "not be validated when no JSON Validator is provided and so just publish onto Rabbit queue" in {
      val jsonPromise = Promise[JValue]()

      val publisher = new Publisher with NoJsonValidator with WithQueue.Consumer with WithRabbit {
        def json(json: JValue) = jsonPromise success json
      }

      publisher.publish("whatever" -> "json")

      jsonPromise.future must beLike[JValue] {
        case _: JValue => ok
      }.awaitFor(10 seconds)
    }
  }
}