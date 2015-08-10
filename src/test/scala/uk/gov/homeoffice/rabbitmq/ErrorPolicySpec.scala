package uk.gov.homeoffice.rabbitmq

import java.io.IOException
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class ErrorPolicySpec extends Specification {
  "Json error" should {
    "have default error policy for no given exception" in new RabbitErrorPolicy with Scope {
      enforce(new Exception) mustEqual Reject
    }

    "have default error policy" in new RabbitErrorPolicy with Scope {
      enforce(new ArithmeticException) mustEqual Reject
    }

    "have alert error policy" in new RabbitErrorPolicy with Scope {
      enforce(new StackOverflowError) mustEqual Alert
    }

    "have retry error policy" in new RabbitErrorPolicy with Scope {
      enforce(new IOException) mustEqual Retry
    }
  }

  "Json error with custom error policy" should {
    trait CustomErrorPolicy extends ErrorPolicy {
      val enforce: PartialFunction[Throwable, ErrorAction] = {
        val enforce: PartialFunction[Throwable, ErrorAction] = {
          case _: IOException => Reject
        }

        enforce orElse RabbitErrorPolicy.enforce
      }
    }

    "override retry error policy to default" in new CustomErrorPolicy with Scope {
      enforce(new IOException) mustEqual Reject
    }

    "not override alert error policy" in new CustomErrorPolicy with Scope {
      enforce(new StackOverflowError) mustEqual Alert
    }
  }
}