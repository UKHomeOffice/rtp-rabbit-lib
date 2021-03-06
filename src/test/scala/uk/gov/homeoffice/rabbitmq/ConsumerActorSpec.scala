package uk.gov.homeoffice.rabbitmq

import scala.concurrent.Promise
import scala.concurrent.duration._
import akka.testkit.TestActorRef
import akka.util.ByteString
import org.json4s.JValue
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import com.rabbitmq.client.Channel
import uk.gov.homeoffice.akka.ActorSystemContext
import uk.gov.homeoffice.json.{JsonError, JsonSchema}

class ConsumerActorSpec(implicit ev: ExecutionEnv) extends Specification with RabbitSpecification with Mockito {
  trait JsonValidator extends uk.gov.homeoffice.json.JsonValidator {
    override val jsonSchema = JsonSchema(parse("""
    {
      "$schema": "http://json-schema.org/draft-04/schema#",
      "id": "http://www.gov.uk/unique-schema-id",
      "type": "object",
      "properties": {
        "valid": {
          "type": "string"
        }
      },
      "additionalProperties": false,
      "required": [
        "valid"
      ]
    }"""))
  }

  "Consumer Actor" should {
    "consume a message that is not JSON and republish it onto an associated error queue" in new ActorSystemContext {
      val jsonErrorPromise = Promise[JsonError]()

      val actor = TestActorRef {
        new ConsumerActor with RabbitErrorPolicy with JsonValidator with Consumer[Any] with WithQueue.ErrorConsumer with WithRabbit {
          def consume(json: JValue) = ??? // Must not be called
          def jsonError(jsonError: JsonError) = jsonErrorPromise success jsonError
        }
      }

      actor.underlyingActor.consume(new RabbitMessage(0, ByteString("unknown"), mock[Channel]), self)

      jsonErrorPromise.future must beLike[JsonError] {
        case JsonError(_, error, _) => ok
      }.awaitFor(10 seconds)
    }

    "consume valid JSON and delegate to a consumer" in new ActorSystemContext {
      val jsonPromise = Promise[JValue]()

      val actor = TestActorRef(new ConsumerActor with RabbitErrorPolicy with JsonValidator with Consumer[JValue] with WithQueue with WithRabbit {
        def consume(json: JValue) = (jsonPromise success json).future
      })

      actor.underlyingActor.consume(new RabbitMessage(0, ByteString(compact(render("valid" -> "json"))), mock[Channel]), self)

      jsonPromise.future must beLike[JValue] {
        case _: JValue => ok
      }.awaitFor(10 seconds)
    }

    "consume invalid JSON i.e. JSON which does not match a given JSON schema and so republish the JSON error onto an associated error queue" in new ActorSystemContext {
      val jsonErrorPromise = Promise[JsonError]()

      val actor = TestActorRef {
        new ConsumerActor with RabbitErrorPolicy with JsonValidator with Consumer[Any] with WithQueue.ErrorConsumer with WithRabbit {
          def consume(json: JValue) = ??? // Must not be called
          def jsonError(jsonError: JsonError) = jsonErrorPromise success jsonError
        }
      }

      actor.underlyingActor.consume(new RabbitMessage(0, ByteString(compact(render("error" -> "json"))), mock[Channel]), self)

      jsonErrorPromise.future must beLike[JsonError] {
        case JsonError(_, error, _) => ok
      }.awaitFor(10 seconds)
    }

    "fail to consume valid JSON and republish it onto associated error queue" in new ActorSystemContext {
      val jsonErrorPromise = Promise[JsonError]()

      val actor = TestActorRef {
        new ConsumerActor with RabbitErrorPolicy with JsonValidator with Consumer[Any] with WithQueue.ErrorConsumer with WithRabbit {
          def consume(json: JValue) = throw new Exception
          def jsonError(jsonError: JsonError) = jsonErrorPromise success jsonError
        }
      }

      actor.underlyingActor.consume(new RabbitMessage(0, ByteString(compact(render("valid" -> "json"))), mock[Channel]), self)

      jsonErrorPromise.future must beLike[JsonError] {
        case JsonError(_, error, _) => ok
      }.awaitFor(10 seconds)
    }

    "fail to consume valid JSON because of a severe issue and republish it to alert queue" in new ActorSystemContext {
      trait AlertErrorPolicy extends ErrorPolicy {
        val enforce: PartialFunction[Throwable, ErrorAction] = {
          case _ => Alert
        }
      }

      val jsonErrorPromise = Promise[JsonError]()

      val actor = TestActorRef {
        new ConsumerActor with AlertErrorPolicy with JsonValidator with Consumer[Any] with WithQueue.AlertConsumer with WithRabbit {
          def consume(json: JValue) = throw new Exception
          def alertError(jsonError: JsonError) = jsonErrorPromise success jsonError
        }
      }

      actor.underlyingActor.consume(new RabbitMessage(0, ByteString(compact(render("valid" -> "json"))), mock[Channel]), self)

      jsonErrorPromise.future must beLike[JsonError] {
        case JsonError(_, error, _) => ok
      }.awaitFor(10 seconds)
    }
  }
}