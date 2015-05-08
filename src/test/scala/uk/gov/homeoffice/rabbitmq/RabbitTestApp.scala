package uk.gov.homeoffice.rabbitmq

import scala.sys.process._

object RabbitTestApp extends App {
  override def main (args: Array[String]) {
    startRabbit

    s1

    stopRabbit
  }

  def startRabbit() = {
    println(s"===> BEFORE <===")
    println("""src/it/resources/run-rabbit.sh RABBITMQ_NODE_PORT=5672 RABBITMQ_SERVER_START_ARGS="-rabbitmq_management listener [{port,15672}]" RABBITMQ_NODENAME=rabbit -detached""".!!)
    println("waiting...")
    "sleep 3".!!
    println("rabbitmqctl -n rabbit status".!!)
  }

  def s1 = {
    println(s"===> TEST <===")

//    val promise = Promise[JValue]()
//
//    val jsonFlat = jsonFromFilepath("caseworker-integration-examples/test/resources/global-entry-flat.json")
//    val json = jsonFromFilepath("caseworker-integration-examples/test/resources/global-entry.json")
//
//    object Blah extends GlobalEntryRouting {
//      val globalEntryVetting = new Vetting with GlobalEntryJsonTransformer with GlobalEntryJsonValidator with GlobalEntryPublisher with WithConsumer with WithRabbit {
//        def consume(body: Array[Byte]) = {
//          promise success parse(new String(body))
//        }
//      }
//    }

//    Post("/global-entry", jsonFlat) ~> Blah.route ~> check {
//      // Response to client
//      status mustEqual OK
//
//      // Response to consumer
//      promise.future must beEqualTo(json).await
//    }
  }

  def stopRabbit() = {
    println(s"===> AFTER <===")
    println(Process("rabbitmqctl -n rabbit stop").! == 0)
  }
}
