package io.kirill.shoppingcart

import scala.concurrent.duration.FiniteDuration
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._

object config {

  final case class AuthConfig(
      jwtSecretKey: String,
      jwtClaim: String,
      tokenExpiration: FiniteDuration,
      passwordSalt: String
  )

  final case class AppConfig(
      auth: AuthConfig
  )

  object AppConfig {
    implicit val appConfig: AppConfig = ConfigSource.default.loadOrThrow[AppConfig]
  }
}
