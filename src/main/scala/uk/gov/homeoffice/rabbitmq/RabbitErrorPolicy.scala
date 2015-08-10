package uk.gov.homeoffice.rabbitmq

import java.io.IOException
import scala.util.control.NonFatal

object RabbitErrorPolicy extends RabbitErrorPolicy

trait RabbitErrorPolicy extends ErrorPolicy {
  val enforce: PartialFunction[Throwable, ErrorAction] = {
    case _: IOException => Retry
    case NonFatal(_) => Reject
    case _ => Alert
  }
}