package uk.gov.homeoffice.rabbitmq

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.util.Try
import com.rabbitmq.client.{Address, Connection, ConnectionFactory}
import com.typesafe.config.{Config, ConfigFactory}
import uk.gov.homeoffice.configuration.HasConfig

trait Rabbit {
  lazy val connection: Connection = Rabbit.connection
}

object Rabbit extends HasConfig {
  lazy val connection: Connection = createConnection

  private[rabbitmq] def createConnection = {
    def address(c: Config): Address = new Address(c.getString("host"), c.getInt("port"))

    val amqpConfig = Try { config.getConfig("amqp") } getOrElse amqpDefaultConfig

    val factory = new ConnectionFactory()
    factory.setAutomaticRecoveryEnabled(amqpConfig.boolean("automatic-recovery", default = true))
    factory.setConnectionTimeout(amqpConfig.duration("timeout", 10 seconds).toMillis.toInt)

    factory.newConnection(amqpConfig.getConfigList("addresses").map(address).toArray)
  }

  private def amqpDefaultConfig = ConfigFactory parseString
  """
    addresses = [{
      host = "127.0.0.1"
      port = 5672
    }]
  """
}