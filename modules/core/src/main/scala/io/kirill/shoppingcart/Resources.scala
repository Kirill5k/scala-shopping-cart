package io.kirill.shoppingcart

import cats.effect.{ConcurrentEffect, ContextShift, Resource}
import cats.implicits._
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import dev.profunktor.redis4cats.log4cats._
import org.typelevel.log4cats.Logger
import io.kirill.shoppingcart.config.AppConfig
import natchez.Trace.Implicits.noop
import skunk.Session

final class Resources[F[_]] private (
    val postgres: Resource[F, Session[F]],
    val redis: RedisCommands[F, String, String]
)

object Resources {

  private def makeRedis[F[_]: ConcurrentEffect: ContextShift: Logger](
      config: AppConfig
  ): Resource[F, RedisCommands[F, String, String]] =
    Redis[F].utf8(s"redis://${config.redis.host}:${config.redis.port}")

  private def makePostgres[F[_]: ConcurrentEffect: ContextShift: Logger](
      config: AppConfig
  ): Resource[F, Resource[F, Session[F]]] =
    Session.pooled[F](
      host = config.postgres.host,
      port = config.postgres.port,
      user = config.postgres.user,
      password = Some(config.postgres.password),
      database = config.postgres.database,
      max = config.postgres.maxConnections
    )

  def make[F[_]: ConcurrentEffect: ContextShift: Logger](
      config: AppConfig
  ): Resource[F, Resources[F]] =
    (makePostgres(config), makeRedis(config)).mapN((p, r) => new Resources[F](p, r))
}
