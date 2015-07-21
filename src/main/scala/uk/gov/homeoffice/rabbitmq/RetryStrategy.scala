package uk.gov.homeoffice.rabbitmq

import scala.concurrent.duration._

class RetryStrategy(var count: Int = 0, var delay: Duration = 10 seconds,
                    incrementStrategy: Duration => Duration = d => d * 2, exceededMaximumRetriesCallback: => Any = ()) {
  import RetryStrategy._

  val maximumNumberOfRetries = 5

  val originalDelay = delay

  def reset() = {
    count = 0
    delay = originalDelay
  }

  def increment: Increment = {
    count = count + 1
    delay = incrementStrategy(delay)

    if (exceededMaximumRetries) {
      exceededMaximumRetriesCallback
      ExceededMaximumRetries
    } else {
      Ok
    }
  }

  def exceededMaximumRetries: Boolean = count > maximumNumberOfRetries
}

object RetryStrategy {
  sealed trait Increment

  object Ok extends Increment

  object ExceededMaximumRetries extends Increment
}