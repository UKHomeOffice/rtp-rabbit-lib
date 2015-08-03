package uk.gov.homeoffice.rabbitmq

import java.io.IOException
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import uk.gov.homeoffice.json.JsonError

class ErrorPolicySpec extends Specification {
  "Json error" should {
    "have default error policy for no given exception" in new DefaultErrorPolicy with Scope {
      enforce(JsonError()) must beAnInstanceOf[Default]
    }

    "have default error policy" in new DefaultErrorPolicy with Scope {
      enforce(JsonError(throwable = Some(new ArithmeticException))) must beAnInstanceOf[Default]
    }

    "have alert error policy" in new DefaultErrorPolicy with Scope {
      enforce(JsonError(throwable = Some(new StackOverflowError))) must beAnInstanceOf[Alert]
    }

    "have retry error policy" in new DefaultErrorPolicy with Scope {
      enforce(JsonError(throwable = Some(new IOException))) must beAnInstanceOf[Retry]
    }
  }

  "Json error with custom error policy" should {
    trait CustomErrorPolicy extends ErrorPolicy {
      val enforce: PartialFunction[JsonError, JsonError with ErrorType] = {
        val enforce: PartialFunction[JsonError, JsonError with ErrorType] = {
          case jsonError @ JsonError(json, error, throwable @ Some(t: IOException)) => new JsonError(json, error, throwable) with Default
        }

        enforce orElse DefaultErrorPolicy.enforce
      }
    }

    "override retry error policy to default" in new CustomErrorPolicy with Scope {
      enforce(JsonError(throwable = Some(new IOException))) must beAnInstanceOf[Default]
    }

    "not override alert error policy" in new CustomErrorPolicy with Scope {
      enforce(JsonError(throwable = Some(new StackOverflowError))) must beAnInstanceOf[Alert]
    }
  }
}