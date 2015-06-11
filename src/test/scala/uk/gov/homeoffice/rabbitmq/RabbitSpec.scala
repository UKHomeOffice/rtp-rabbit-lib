package uk.gov.homeoffice.rabbitmq

import com.rabbitmq.client.Connection
import org.specs2.mutable.Specification
import org.specs2.specification.AfterEach

trait RabbitSpec extends Rabbit with AfterEach {
  self: Specification =>

  isolated

  override lazy val connection: Connection = {
    val conn = Rabbit.createConnection
    println(s"+ Opened Rabbit connection: $conn (${conn.hashCode()})")
    conn
  }

  protected def after = {
    connection.close()
    println(s"x Closed Rabbit connection: $connection (hashCode: ${connection.hashCode()})")
  }

  /**
   * Not nice to name a trait prefixed by "With" as it will probably mixed in using "with".
   * However, this seems to be a naming idiom (certainly from Play) to distinguish this trait that is only for testing as opposed to say main code named "Rabbit"
   */
  protected trait WithRabbit extends Rabbit {
    override lazy val connection: Connection = self.connection
  }
}