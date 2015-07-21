package uk.gov.homeoffice.rabbitmq

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import akka.testkit.{TestActorRef, TestProbe}
import org.json4s._
import org.scalactic.{Good, Bad}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import uk.gov.homeoffice.akka.ActorSystemContext
import uk.gov.homeoffice.concurrent.CountDownLatch
import uk.gov.homeoffice.json.{JsonError, NoJsonValidator}
import uk.gov.homeoffice.rabbitmq.RetryStrategy.ExceededMaximumRetries

class ConsumerActorRetrySpec(implicit ev: ExecutionEnv) extends Specification with RabbitSpecification {
  "Consumer Actor" should {
    "retry" in new ActorSystemContext {
      val retries = CountDownLatch(2)

      TestActorRef {
        new ConsumerActor with NoJsonValidator with Consumer[Any] with Publisher with WithQueue with WithRabbit {
          override val retryStrategy = new RetryStrategy(delay = 1 second, maximumNumberOfRetries = 3, incrementStrategy = _ => 1 second)

          override def consume(json: JValue) = {
            retries countDown()
            Future.successful { Bad(JsonError(error = "", throwable = Some(RetryThrowable(new Exception)))) }
          }

          publish(JObject())
        }
      }

      retries.await(10 seconds) must beTrue
    }

    "exceed maximum number of retries and so be terminated" in new ActorSystemContext {
      val exceededMaximumNumberOfRetries = Promise[Boolean]()

      val actor = TestActorRef {
        new ConsumerActor with NoJsonValidator with Consumer[Any] with Publisher with WithQueue with WithRabbit {
          override val retryStrategy = new RetryStrategy(delay = 1 second, maximumNumberOfRetries = 3, incrementStrategy = _ => 1 second) {
            override def increment = {
              val incrementResult = super.increment
              if (incrementResult == ExceededMaximumRetries) exceededMaximumNumberOfRetries success true
              incrementResult
            }
          }

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

    "retry twice and succeed the third time" in new ActorSystemContext {
      val retried = CountDownLatch(2)
      val successfulRetry = Promise[Boolean]()

      val actor = TestActorRef {
        new ConsumerActor with NoJsonValidator with Consumer[Any] with Publisher with WithQueue with WithRabbit {
          override val retryStrategy = new RetryStrategy(delay = 1 second, maximumNumberOfRetries = 3, incrementStrategy = _ => 1 second)

          override def consume(json: JValue) = {
            retried countDown()

            if (retried.isZero) {
              successfulRetry success true
              Future.successful { Good(json) }
            } else {
              Future.successful { Bad(JsonError(error = "", throwable = Some(RetryThrowable(new Exception)))) }
            }
          }

          publish(JObject())
        }
      }

      retried.await(10 seconds) must beTrue
      successfulRetry.future must beTrue.awaitFor(10 seconds)
    }
  }
}