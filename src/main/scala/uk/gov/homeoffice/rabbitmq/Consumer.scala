package uk.gov.homeoffice.rabbitmq

import scala.concurrent.Future
import org.json4s._
import org.scalactic.Or
import uk.gov.homeoffice.json.JsonError
import uk.gov.homeoffice.or.OrOps

trait Consumer[T] extends OrOps {
  /**
   *
   * @param json JValue to be consumed/processed/transformed
   * @return a transformation of the given JValue or a JsonError, where "Or" is used for flexibilty to allow for "bad" processing.
   * Note that throwing an exception within an implementation is taken care of by the fact that a Future (like a Try) has a Success and Failure outcome.
   */
  def consume(json: JValue): Future[T Or JsonError]
}