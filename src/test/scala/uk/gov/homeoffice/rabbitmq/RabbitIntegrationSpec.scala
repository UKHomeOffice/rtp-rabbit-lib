/*
package uk.gov.homeoffice.rabbitmq

import scala.concurrent.Promise
import scala.sys.process._
import spray.http.StatusCodes._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.specs2.specification._
import uk.gov.homeoffice.json.Json
import uk.gov.homeoffice.vet.Vetting
import uk.gov.homeoffice.vet.json.{GlobalEntryJsonTransformer, GlobalEntryJsonValidator}
import uk.gov.homeoffice.vet.registeredtraveller.publish.{GlobalEntryPublisher, WithConsumer}
import uk.gov.homeoffice.vet.registeredtraveller.routing.{GlobalEntryRouting, RouteSpecification}

class RabbitIntegrationSpec extends RouteSpecification with RabbitSpec with Json {
  trait Context extends Scope with GlobalEntryRouting {
    println("src/it/resources/run-rabbit.sh RABBITMQ_NODE_PORT=5672 RABBITMQ_NODENAME=rabbit -detached".!!)
    println("waiting...")
    "sleep 3".!!
    println("rabbitmqctl -n rabbit status".!!)

    val promise = Promise[JValue]()

    val jsonFlat = jsonFromFilepath("caseworker-integration-examples/test/resources/global-entry-flat.json")
    val json = jsonFromFilepath("caseworker-integration-examples/test/resources/global-entry.json")

    override val globalEntryVetting = new Vetting with GlobalEntryJsonTransformer with GlobalEntryJsonValidator
      with GlobalEntryPublisher with WithConsumer with WithRabbit {
      def consume(body: Array[Byte]) = {
        promise success parse(new String(body))
      }
    }
  }

  "A miracle" should {
    "submit valid JSON and be consumed" in new Context {
      Post("/registered-traveller/global-entry", jsonFlat) ~> route ~> check {
        // Response to client
        status mustEqual OK

        // Response to consumer
        promise.future must beEqualTo(json).await
      }

      "sleep 5".!!
      println(Process("rabbitmqctl -n rabbit stop").! == 0)
      ok
    }
  }
}*/
