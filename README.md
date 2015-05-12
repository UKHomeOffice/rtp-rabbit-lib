Rabbit - Scala publish/subscribe for RabbitMQ
=============================================
Scala general functionality to interface with RabbitMQ (originally written for Registered Traveller UK)

Project built with the following (main) technologies:

- Scala

- SBT

- RabbitMQ

Introduction
------------
TODO

Build and Deploy
----------------
The project is built with SBT (using Activator on top).

To compile:
> activator compile

To run the specs:
> activator test

To run integration specs:
> activator it:test

The project can be "assembled" into a "one JAR":
> activator assembly

Note that "assembly" will first compile and test.

Publishing
----------
To publish the jar to artifactory you will need to 

1. Copy the .credentials file into your <home directory>/.ivy2/
2. Edit this .credentials file to fill in the artifactory user and password

> activator publish

Example Usage
-------------
```scala
  object ExampleBoot extends App with HasConfig {
    implicit val json4sFormats = DefaultFormats
  
    val system = ActorSystem("importer-actor-system", config)
  
    // Consume
    system.actorOf(Props(new ConsumerActor with Consumer[String] with ExampleQueue with Rabbit {
      def consume(json: JValue) = Future {
        val message = (json \ "message").extract[String]
        println(s"Congratulations, consumed message '$message'")
        Good(message)
      }
    }))
  
    // Publish
    val publisher = new Publisher with ExampleQueue with Rabbit
    publisher.publish(JObject("message" -> JString("hello world!")))
  }
  
  trait ExampleQueue extends Queue {
    def queueName = "rabb-it-example"
  }
```

Noting that a "configuration" such as application.conf must be provided e.g.
```scala
  amqp {
    addresses = [{
      host = "127.0.0.1"
      port = 5672
    }]
  
    automatic-recovery = on
  }
```