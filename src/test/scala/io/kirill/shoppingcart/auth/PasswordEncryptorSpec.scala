package io.kirill.shoppingcart.auth

import java.util.concurrent.TimeUnit

import cats.effect.IO
import io.kirill.shoppingcart.CatsIOSpec
import io.kirill.shoppingcart.auth.user.Password
import io.kirill.shoppingcart.config.AuthConfig

import scala.concurrent.duration.FiniteDuration

class PasswordEncryptorSpec extends CatsIOSpec {

  "A basic PasswordEncrypter" - {

    "hash and validate password with salt" in {
      val config = AuthConfig(
        "secret-key",
        "claim",
        FiniteDuration(10, TimeUnit.DAYS),
        "$2a$10$8K1p/a0dL1LXMIgoEDFrwO"
      )

      val result = for {
        e       <- IO(PasswordEncryptor[IO](config))
        hash    <- e.hash(Password("Password123!"))
        isValid <- e.isValid(Password("Password123!"), hash)
      } yield isValid

      result.asserting { isValid =>
        isValid must be(true)
      }
    }
  }
}
