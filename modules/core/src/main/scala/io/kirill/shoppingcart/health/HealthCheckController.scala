package io.kirill.shoppingcart.health

import cats.effect.Sync
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import io.circe.generic.auto._
import io.kirill.shoppingcart.common.web.RestController
import io.kirill.shoppingcart.common.json._
import org.http4s.circe._
import org.http4s.HttpRoutes
import org.http4s.server.Router

final class HealthCheckController[F[_]: Sync: Logger](
    healthCheckService: HealthCheckService[F]
) extends RestController[F] {
  import HealthCheckController._

  private val prefixPath = "/healthcheck"

  private val httpRoutes: HttpRoutes[F] =
    HttpRoutes.of {
      case GET -> Root / "status" =>
        withErrorHandling {
          Ok(healthCheckService.status.map(HealthCheckResponse.from))
        }
    }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}

object HealthCheckController {
  final case class HealthCheckResponse(redis: String, postgres: String)

  object HealthCheckResponse {
    def from(appStatus: AppStatus): HealthCheckResponse = {
      HealthCheckResponse(
        if (appStatus.redis.value) "up" else "down",
        if (appStatus.postgres.value) "up" else "down"
      )
    }
  }
}
