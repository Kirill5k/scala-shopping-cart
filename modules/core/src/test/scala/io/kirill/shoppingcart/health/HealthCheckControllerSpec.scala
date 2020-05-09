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

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)

  "A HealthCheckController" should {

    "return status of the app" in {
      val healthCheckServiceMock = mock[HealthCheckService[IO]]
      val controller             = new HealthCheckController[IO](healthCheckServiceMock)

      when(healthCheckServiceMock.status).thenReturn(IO.pure(AppStatus(PostgresStatus(true), RedisStatus(false))))

      val request = Request[IO](uri = uri"/healthcheck/status")
      val response: IO[Response[IO]] = controller.routes.orNotFound.run(request)

      verifyResponse[HealthCheckResponse](response, Status.Ok, Some(HealthCheckResponse("down", "up")))
    }
  }
}
