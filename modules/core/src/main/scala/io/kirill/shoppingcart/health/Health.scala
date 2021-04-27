package io.kirill.shoppingcart.health

import cats.Parallel
import cats.effect.{Concurrent, Timer}
import cats.implicits._
import io.kirill.shoppingcart.Resources
import org.typelevel.log4cats.Logger

final case class ServiceStatus(value: Boolean) extends AnyVal

final case class AppStatus(
    postgres: ServiceStatus,
    redis: ServiceStatus
)

final class Health[F[_]] private (
    val healthCheckController: HealthCheckController[F]
)

object Health {
  def make[F[_]: Concurrent: Parallel: Timer: Logger](res: Resources[F]): F[Health[F]] =
    for {
      service    <- HealthCheckService.make(res.postgres, res.redis)
      controller <- HealthCheckController.make(service)
    } yield new Health[F](controller)
}
