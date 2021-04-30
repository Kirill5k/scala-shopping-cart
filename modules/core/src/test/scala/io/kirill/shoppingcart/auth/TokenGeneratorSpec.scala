package io.kirill.shoppingcart.auth

import cats.effect.{Blocker, IO}
import dev.profunktor.auth.jwt.JwtToken
import io.kirill.shoppingcart.CatsIOSpec
import io.kirill.shoppingcart.config.{AdminJwtConfig, AppConfig, AuthConfig, UserJwtConfig}

import scala.concurrent.duration._

class TokenGeneratorSpec extends CatsIOSpec {

  val authConfig = AuthConfig(
    "$2a$10$8K1p/a0dL1LXMIgoEDFrwO",
    UserJwtConfig("user-secret-key", 30.minutes),
    AdminJwtConfig("admin-secret-key", "admin-token", "admin-claim")
  )

  "A TokenGenerator should" - {

    "generate tokens" in {

      val result = for {
        gen   <- TokenGenerator.make[IO](authConfig)
        token <- gen.generate
      } yield token

      result.asserting { t =>
        t mustBe a[JwtToken]
      }
    }
  }
}
