package uk.gov.homeoffice.rabbitmq

import java.util.concurrent.TimeUnit
import scala.collection.JavaConversions._
import scala.util.Try
import com.rabbitmq.client.{Address, Connection, ConnectionFactory}
import com.typesafe.config.{Config, ConfigFactory}
import uk.gov.homeoffice.HasConfig

trait Rabbit {
  lazy val connection: Connection = Rabbit.connection
}

object Rabbit extends HasConfig {
  lazy val connection: Connection = createConnection

  private[rabbitmq] def createConnection = {
    def address(c: Config): Address = new Address(c.getString("host"), c.getInt("port"))

    val amqpConfig = Try { config.getConfig("amqp") } getOrElse amqpDefaultConfig

    val factory = new ConnectionFactory()
    factory.setAutomaticRecoveryEnabled(Try { amqpConfig.getBoolean("automatic-recovery") } getOrElse true)
    factory.setConnectionTimeout(Try { amqpConfig.getDuration("timeout", TimeUnit.MILLISECONDS).toInt } getOrElse 10000)

    factory.newConnection(amqpConfig.getConfigList("addresses").map(address).toArray)
  }

  private def amqpDefaultConfig = ConfigFactory.parseString("""
    addresses = [{
      host = "127.0.0.1"
      port = 5672
    }]

    automatic-recovery = on

    timeout = 10000 milliseconds""")
}