package io.kirill.shoppingcart

import cats.Parallel
import cats.effect.{Concurrent, Sync, Timer}
import cats.implicits._
import io.chrisdavenport.log4cats.Logger

package object health {

  final case class RedisStatus(value: Boolean)    extends AnyVal
  final case class PostgresStatus(value: Boolean) extends AnyVal

  final case class AppStatus(
      postgres: PostgresStatus,
      redis: RedisStatus
  )

  final class Health[F[_]: Sync](
      val healthCheckController: HealthCheckController[F]
  )

  object Health {
    def make[F[_]: Concurrent: Parallel: Timer: Logger](res: Resources[F]): F[Health[F]] =
      for {
        service <- HealthCheckService.make(res.postgres, res.redis)
        controller <- HealthCheckController.make(service)
      } yield new Health[F](controller)
  }
}
