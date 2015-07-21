package uk.gov.homeoffice.rabbitmq

import scala.concurrent.duration._

/**
 * A mutable class to track retries against failed consumption of Rabbit messages, where the exception has been deemed to retry the message consumption.
 * Note that this class is mutable and not thread safe, being initially intended to be only used by [[uk.gov.homeoffice.rabbitmq.ConsumerActor]]
 * @param delay Duration until next retry
 * @param numberOfRetries Int current number of retries
 * @param maximumNumberOfRetries Int the maximum number of retries
 * @param incrementStrategy Function[Duration => Duration] which dictates how the delay Duration is calculated.
 */
class RetryStrategy(var delay: Duration = 10 seconds, var numberOfRetries: Int = 0,
                    maximumNumberOfRetries: Int = 10,
                    incrementStrategy: Duration => Duration = d => d * 2) {
  import RetryStrategy._
  
  val originalDelay = delay

  def reset() = {
    numberOfRetries = 0
    delay = originalDelay
  }

  def increment: IncrementResult = {
    numberOfRetries = numberOfRetries + 1
    delay = incrementStrategy(delay)

    if (exceededMaximumRetries) {
      ExceededMaximumRetries
    } else {
      Ok
    }
  }

  def exceededMaximumRetries: Boolean = numberOfRetries > maximumNumberOfRetries
}

object RetryStrategy {
  sealed trait IncrementResult

  object Ok extends IncrementResult

  object ExceededMaximumRetries extends IncrementResult
}