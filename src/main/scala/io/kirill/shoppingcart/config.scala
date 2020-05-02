package io.kirill.shoppingcart

import scala.concurrent.duration.FiniteDuration

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
}
