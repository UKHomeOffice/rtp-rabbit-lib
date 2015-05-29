package uk.gov.homeoffice.rabbitmq

import scala.concurrent.Future
import org.json4s._
import org.scalactic.Or
import uk.gov.homeoffice.json.JsonError

trait Consumer[T] {
  def consume(json: JValue): Future[T Or JsonError]
}