package io.kirill.shoppingcart.auth

import cats.effect.IO
import dev.profunktor.auth.jwt.JwtToken
import io.kirill.shoppingcart.CatsIOSpec
import io.kirill.shoppingcart.config.AppConfig

class TokenGeneratorSpec extends CatsIOSpec {

  "A basic TokenGenerator" - {

    "generate tokens" in {
      import AppConfig.appConfig

      val result = for {
        gen       <- TokenGenerator.make[IO]
        token    <- gen.generate
      } yield token

      result.asserting { t =>
        t mustBe a [JwtToken]
      }
    }
  }
}
