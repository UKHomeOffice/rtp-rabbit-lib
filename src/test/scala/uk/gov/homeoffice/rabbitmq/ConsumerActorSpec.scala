package uk.gov.homeoffice.rabbitmq

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import akka.testkit.TestActorRef
import akka.util.ByteString
import org.json4s.JValue
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.scalactic.{Bad, Good}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import com.rabbitmq.client.Channel
import uk.gov.homeoffice.akka.ActorSystemContext
import uk.gov.homeoffice.json.{JsonError, JsonSchema, JsonValidator}

class ConsumerActorSpec(implicit ev: ExecutionEnv) extends Specification with RabbitSpecification with Mockito {
  trait TestJsonValidator extends JsonValidator {
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
      val invalidDataConsumed = Promise[Boolean]()

      val actor = TestActorRef(new ConsumerActor with WithConsumer with WithErrorConsumer with TestJsonValidator with WithQueue with WithRabbit {
        def consumeError(body: Array[Byte]) = invalidDataConsumed success true
      })

      actor.underlyingActor.consume(new RabbitMessage(0, ByteString("unknown"), mock[Channel]), self)

      invalidDataConsumed.future must beTrue.awaitFor(10 seconds)
    }

    "consume valid JSON and delegate to a consumer" in new ActorSystemContext {
      val validJsonConsumed = Promise[Boolean]()

      val actor = TestActorRef(new ConsumerActor with Consumer[Boolean] with TestJsonValidator with WithQueue with WithRabbit {
        def consume(json: JValue) = {
          validJsonConsumed success true
          Future.successful(Good(true))
        }
      })

      actor.underlyingActor.consume(new RabbitMessage(0, ByteString(compact(render("valid" -> "json"))), mock[Channel]), self)

      validJsonConsumed.future must beTrue.awaitFor(10 seconds)
    }

    "consume invalid JSON i.e. JSON which does not match a given JSON schema and so republish the JSON error onto an associated error queue" in new ActorSystemContext {
      val invalidJsonConsumed = Promise[Boolean]()

      val actor = TestActorRef(new ConsumerActor with WithConsumer with WithErrorConsumer with TestJsonValidator with WithQueue with WithRabbit {
        def consumeError(body: Array[Byte]) = invalidJsonConsumed success true
      })

      actor.underlyingActor.consume(new RabbitMessage(0, ByteString(compact(render("error" -> "json"))), mock[Channel]), self)

      invalidJsonConsumed.future must beTrue.awaitFor(10 seconds)
    }

    "fail to consume valid JSON and republish it onto associated error queue" in new ActorSystemContext {
      val errorConsumed = Promise[Boolean]()

      val actor = TestActorRef(new ConsumerActor with WithConsumer with WithErrorConsumer with TestJsonValidator with WithQueue with WithRabbit {
        override def consume(json: JValue) = {
          Future.successful(Bad(JsonError(error = "whoops")))
        }

        def consumeError(body: Array[Byte]) = errorConsumed success true
      })

      actor.underlyingActor.consume(new RabbitMessage(0, ByteString(compact(render("valid" -> "json"))), mock[Channel]), self)

      errorConsumed.future must beTrue.awaitFor(10 seconds)
    }
  }
}