package io.kirill.shoppingcart

import cats.effect.{ConcurrentEffect, ContextShift, Resource, Sync}
import cats.implicits._
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import dev.profunktor.redis4cats.log4cats._
import org.typelevel.log4cats.Logger
import io.kirill.shoppingcart.config.AppConfig
import natchez.Trace.Implicits.noop
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import skunk.Session

import scala.concurrent.ExecutionContext

final class Resources[F[_]] private (
    val postgres: Resource[F, Session[F]],
    val redis: RedisCommands[F, String, String],
    val client: Client[F]
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

  private def makeClient[F[_]: ConcurrentEffect](ec: ExecutionContext): Resource[F, Client[F]] =
    BlazeClientBuilder[F](ec).resource

  def make[F[_]: ConcurrentEffect: ContextShift: Logger](
      config: AppConfig,
      ec: ExecutionContext
  ): Resource[F, Resources[F]] =
    (makePostgres(config), makeRedis(config), makeClient(ec)).mapN((p, r, c) => new Resources[F](p, r, c))
}
