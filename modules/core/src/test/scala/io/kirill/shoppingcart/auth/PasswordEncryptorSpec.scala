package io.kirill.shoppingcart.auth


import cats.effect.IO
import io.kirill.shoppingcart.CatsIOSpec
import io.kirill.shoppingcart.auth.user.Password
import io.kirill.shoppingcart.config.AppConfig

class PasswordEncryptorSpec extends CatsIOSpec {

  "A basic PasswordEncrypter" - {

    "hash and validate password with salt" in {
      import AppConfig.appConfig

      val result = for {
        e       <- PasswordEncryptor.make[IO]
        hash    <- e.hash(Password("Password123!"))
        isValid <- e.isValid(Password("Password123!"), hash)
      } yield isValid

      result.asserting { isValid =>
        isValid must be(true)
      }
    }
  }
}
