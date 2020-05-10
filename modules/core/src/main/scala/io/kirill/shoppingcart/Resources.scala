package io.kirill.shoppingcart

import cats.effect.{ConcurrentEffect, ContextShift, Resource}
import cats.implicits._
import dev.profunktor.redis4cats.algebra.RedisCommands
import dev.profunktor.redis4cats.connection.{RedisClient, RedisURI}
import dev.profunktor.redis4cats.domain.RedisCodec
import dev.profunktor.redis4cats.interpreter.Redis
import dev.profunktor.redis4cats.log4cats._
import io.chrisdavenport.log4cats.Logger
import io.kirill.shoppingcart.config.AppConfig
import natchez.Trace.Implicits.noop
import skunk.Session

final case class Resources[F[_]](
  postgres: Resource[F, Session[F]],
  redis: RedisCommands[F, String, String]
)

object Resources {

  private def makeRedis[F[_]: ConcurrentEffect: ContextShift: Logger](
      implicit config: AppConfig
  ): Resource[F, RedisCommands[F, String, String]] =
    for {
      uri    <- Resource.liftF(RedisURI.make[F](s"redis://${config.redis.host}:${config.redis.port}"))
      client <- RedisClient[F](uri)
      redis  <- Redis[F, String, String](client, RedisCodec.Utf8)
    } yield redis

  private def makePostgres[F[_]: ConcurrentEffect: ContextShift: Logger](
      implicit config: AppConfig
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
      implicit config: AppConfig
  ): Resource[F, Resources[F]] =
    (makePostgres, makeRedis).mapN(Resources.apply[F])
}
