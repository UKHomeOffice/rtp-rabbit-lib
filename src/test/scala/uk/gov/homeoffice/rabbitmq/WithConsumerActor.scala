package uk.gov.homeoffice.rabbitmq

import scala.concurrent.Future
import org.json4s.JValue
import org.scalactic.{Or, Bad}
import uk.gov.homeoffice.json.{JsonError, JsonValidator}

class WithConsumerActor extends ConsumerActor with Consumer[Any] {
  this: Consumer[Any] with JsonValidator with Queue with Rabbit =>

  def consume(json: JValue): Future[Any Or JsonError] = Future.successful(Bad(JsonError(error = "Consumed by WithConsumerActor - you should override this is used as part of your test to be explicit")))
}