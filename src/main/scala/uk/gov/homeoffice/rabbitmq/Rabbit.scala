package uk.gov.homeoffice.rabbitmq

import java.util.concurrent.TimeUnit
import scala.collection.JavaConversions._
import com.rabbitmq.client.{Address, Connection, ConnectionFactory}
import com.typesafe.config.Config
import uk.gov.homeoffice.HasConfig

trait Rabbit {
  lazy val connection: Connection = Rabbit.connection
}

object Rabbit extends HasConfig {
  lazy val connection: Connection = createConnection

  private[rabbitmq] def createConnection = {
    def address(c: Config): Address = new Address(c.getString("host"), c.getInt("port"))

    val amqpConfig = config.getConfig("amqp")

    val factory = new ConnectionFactory()
    factory.setAutomaticRecoveryEnabled(amqpConfig.getBoolean("automatic-recovery"))
    factory.setConnectionTimeout(amqpConfig.getDuration("timeout", TimeUnit.MILLISECONDS).toInt)

    factory.newConnection(amqpConfig.getConfigList("addresses").map(address).toArray)
  }
}