package uk.gov.homeoffice.rabbitmq

import scala.concurrent.Promise
import scala.concurrent.duration._
import akka.testkit.TestActorRef
import akka.util.ByteString
import org.json4s.JValue
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import com.rabbitmq.client.Channel
import uk.gov.homeoffice.akka.ActorSystemContext
import uk.gov.homeoffice.json.{JsonError, JsonSchema}
import uk.gov.homeoffice.rabbitmq.RabbitMessage.KO

class ConsumerActorRabbitMessageSpec(implicit ev: ExecutionEnv) extends Specification with RabbitSpecification with Mockito {
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
    trait Context extends ActorSystemContext {
      val jsonErrorPromise = Promise[JsonError]()

      val actor = TestActorRef {
        new ConsumerActor with RabbitErrorPolicy with JsonValidator with Consumer[Any] with WithQueue.ErrorConsumer with WithRabbit {
          def consume(json: JValue) = ??? // Should not be called
          def jsonError(jsonError: JsonError) = jsonErrorPromise success jsonError
        }
      }
    }

    "be re-published to an error queue because of JSON message that fails validation" in new Context {
      actor ! new RabbitMessage(0, ByteString(compact(render("valid" -> 0))), mock[Channel])
      expectMsg(KO)

      jsonErrorPromise.future must beLike[JsonError] {
        case j: JsonError => ok
      }.awaitFor(10 seconds)
    }

    "be re-published to an error queue because of message that is not JSON" in new Context {
      actor ! new RabbitMessage(0, ByteString("This is not JSON"), mock[Channel])
      expectMsg(KO)

      jsonErrorPromise.future must beLike[JsonError] {
        case j: JsonError => ok
      }.awaitFor(10 seconds)
    }

    "be re-published to an error queue because consumer encountered an exception" in new ActorSystemContext {
      val jsonErrorPromise = Promise[JsonError]()

      val actor = TestActorRef {
        new ConsumerActor with RabbitErrorPolicy with JsonValidator with Consumer[Any] with WithQueue.ErrorConsumer with WithRabbit {
          def consume(json: JValue) = throw new Exception
          def jsonError(jsonError: JsonError) = jsonErrorPromise success jsonError
        }
      }

      actor ! new RabbitMessage(0, ByteString(compact(render("valid" -> "whatever"))), mock[Channel])
      expectMsg(KO)

      jsonErrorPromise.future must beLike[JsonError] {
        case j: JsonError => ok
      }.awaitFor(10 seconds)
    }
  }
}