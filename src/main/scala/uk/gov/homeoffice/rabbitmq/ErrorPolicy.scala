package uk.gov.homeoffice.rabbitmq

/**
 * Stipulate how a Throwable should be handled by enforcing a particular policy on a Throwable, to be mapped to a required action.
 */
trait ErrorPolicy {
  def enforce: PartialFunction[Throwable, ErrorAction]
}

/**
 * This trait has not been sealed to allow for custom extensions which could then be used in custom error policies
 */
trait ErrorAction

/**
 * Consider this action as the default i.e. the error should be noted (logged etc.) and thrown away.
 */
object Reject extends ErrorAction

/**
 * As with the default case of noting and throwing away an error, the type of error associated with this action should somehow bring the error to attention and not simple log the error.
 */
object Alert extends ErrorAction

/**
 * The error encountered should have the processing, prior to the error, retried.
 */
object Retry extends ErrorAction