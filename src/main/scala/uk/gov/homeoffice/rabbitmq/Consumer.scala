package uk.gov.homeoffice.rabbitmq

import scala.concurrent.Future
import org.json4s._
import org.scalactic.Or
import uk.gov.homeoffice.json.JsonError

trait Consumer[T] {
  /**
   *
   * @param json JValue to be consumed/processed/transformed
   * @return a transformation of the given JValue or a JsonError, which just adds flexibility with regards to this library:
   * A library for Rabbit where consumption may be combined with publication and publishing could go to an error queue for example i.e. error JSON
   */
  def consume(json: JValue): Future[T Or JsonError]
}