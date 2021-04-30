package io.kirill.shoppingcart.health

import cats.Parallel
import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import dev.profunktor.redis4cats.RedisCommands
import skunk._
import skunk.codec.all._
import skunk.implicits._
import scala.concurrent.duration._

sealed trait HealthCheckService[F[_]] {
  def status: F[AppStatus]
}

final private class LiveHealthCheckService[F[_]: Concurrent: Parallel: Timer](
    session: Resource[F, Session[F]],
    redis: RedisCommands[F, String, String]
) extends HealthCheckService[F] {

  private val q: Query[Void, Int] =
    sql"SELECT pid FROM pg_stat_activity".query(int4)

  private def postgresHealth: F[AppStatus.Service] =
    session
      .use(_.execute(q))
      .map(_.nonEmpty)
      .timeout(3.second)
      .orElse(false.pure[F])
      .map(AppStatus.Service.apply)

  private def redisHealth: F[AppStatus.Service] =
    redis.ping
      .map(_.nonEmpty)
      .timeout(3.second)
      .orElse(false.pure[F])
      .map(AppStatus.Service.apply)

  override def status: F[AppStatus] =
    (postgresHealth, redisHealth).parMapN(AppStatus.apply)
}

object HealthCheckService {
  def make[F[_]: Concurrent: Parallel: Timer](
      session: Resource[F, Session[F]],
      redis: RedisCommands[F, String, String]
  ): F[HealthCheckService[F]] =
    Sync[F].pure(new LiveHealthCheckService[F](session, redis))
}
