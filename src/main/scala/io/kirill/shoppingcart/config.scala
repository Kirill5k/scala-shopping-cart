package io.kirill.shoppingcart

import scala.concurrent.duration.FiniteDuration

object config {

  case class SecurityConfig(
      jwtSecretKey: String,
      jwtClaim: String,
      tokenExpiration: FiniteDuration,
      passwordSalt: String
  )
}
