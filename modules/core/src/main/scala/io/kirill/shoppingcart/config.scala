package io.kirill.shoppingcart

import scala.concurrent.duration.FiniteDuration
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._

object config {

  final case class RedisConfig(
      host: String,
      port: Int
  )

  final case class PostgresConfig(
      host: String,
      port: Int,
      user: String,
      password: String,
      database: String,
      maxConnections: Int
  )

  final case class AuthConfig(
      jwtSecretKey: String,
      jwtClaim: String,
      tokenExpiration: FiniteDuration,
      passwordSalt: String
  )

  final case class ShopConfig(
      cartExpiration: FiniteDuration
  )

  final case class AppConfig(
      auth: AuthConfig,
      shop: ShopConfig,
      redis: RedisConfig,
      postgres: PostgresConfig
  )

  object AppConfig {
    implicit val appConfig: AppConfig = ConfigSource.default.loadOrThrow[AppConfig]
  }
}
