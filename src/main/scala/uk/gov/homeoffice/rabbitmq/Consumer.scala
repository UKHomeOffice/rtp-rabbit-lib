package uk.gov.homeoffice.rabbitmq

import scala.concurrent.Future
import org.json4s._

trait Consumer[T] {
  /**
   *
   * @param json JValue to be consumed/processed/transformed.
   * @return Result of transformation.
   * As the contract is a Future, errors can be handled in the wrapped up Failure case (much like a Try),
   * and as consuming can take a while and use external resources, a Future is the best fit all round.
   */
  def consume(json: JValue): Future[T]
}