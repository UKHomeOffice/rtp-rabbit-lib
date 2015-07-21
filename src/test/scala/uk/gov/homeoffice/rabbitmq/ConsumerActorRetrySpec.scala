package uk.gov.homeoffice.rabbitmq

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import akka.testkit.{TestActorRef, TestProbe}
import org.json4s._
import org.scalactic.Bad
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import uk.gov.homeoffice.akka.ActorSystemContext
import uk.gov.homeoffice.concurrent.CountDownLatch
import uk.gov.homeoffice.json.{JsonError, NoJsonValidator}

class ConsumerActorRetrySpec(implicit ev: ExecutionEnv) extends Specification with RabbitSpecification {
  "Consumer Actor" should {
    "retry" in new ActorSystemContext {
      val retries = CountDownLatch(2)

      TestActorRef {
        new ConsumerActor with NoJsonValidator with Consumer[Any] with Publisher with WithQueue with WithRabbit {
          override val retryStrategy = new RetryStrategy(delay = 1 second, incrementStrategy = _ => 1 second)

          override def consume(json: JValue) = {
            retries countDown()
            Future.successful { Bad(JsonError(error = "", throwable = Some(RetryThrowable(new Exception)))) }
          }

          publish(JObject())
        }
      }

      retries.await(30 seconds) must beTrue
    }

    "exceed maximum number of retries and so be terminated" in new ActorSystemContext {
      val exceededMaximumNumberOfRetries = Promise[Boolean]()

      val actor = TestActorRef {
        new ConsumerActor with NoJsonValidator with Consumer[Any] with Publisher with WithQueue with WithRabbit {
          override val retryStrategy = new RetryStrategy(delay = 1 second,
                                                         incrementStrategy = _ => 1 second,
                                                         exceededMaximumRetriesCallback = exceededMaximumNumberOfRetries success true)

          override def consume(json: JValue) = Future.successful {
            Bad(JsonError(error = "", throwable = Some(RetryThrowable(new Exception))))
          }

          publish(JObject())
        }
      }

      val actorWatcher = TestProbe()
      actorWatcher.watch(actor)

      exceededMaximumNumberOfRetries.future must beTrue.awaitFor(10 seconds)
      actorWatcher.expectTerminated(actor, 10 seconds)
    }
  }
}