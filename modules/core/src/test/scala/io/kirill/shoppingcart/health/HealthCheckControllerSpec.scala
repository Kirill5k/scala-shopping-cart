package io.kirill.shoppingcart.health

import cats.effect.{ContextShift, IO}
import io.circe.generic.auto._
import io.kirill.shoppingcart.health.HealthCheckController.HealthCheckResponse
import io.kirill.shoppingcart.{CatsIOSpec, ControllerSpec}
import io.kirill.shoppingcart.common.json._
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._

import scala.concurrent.ExecutionContext

class HealthCheckControllerSpec extends ControllerSpec {

  "A HealthCheckController" should {

    "return status of the app" in {
      val service    = mock[HealthCheckService[IO]]
      val controller = new HealthCheckController[IO](service)

      when(service.status).thenReturn(IO.pure(AppStatus(AppStatus.Service(true), AppStatus.Service(false))))

      val request  = Request[IO](uri = uri"/health/status")
      val response = controller.routes.orNotFound.run(request)

      verifyResponse[HealthCheckResponse](response, Status.Ok, Some(HealthCheckResponse("down", "up")))
    }
  }
}
