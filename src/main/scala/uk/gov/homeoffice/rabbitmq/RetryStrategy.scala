package uk.gov.homeoffice.rabbitmq

import scala.concurrent.duration._

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