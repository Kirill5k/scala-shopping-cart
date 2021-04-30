package io.kirill.shoppingcart.auth

import cats.effect.{Blocker, IO}
import io.kirill.shoppingcart.CatsIOSpec
import io.kirill.shoppingcart.auth.user.User
import io.kirill.shoppingcart.config.{AdminJwtConfig, AppConfig, AuthConfig, UserJwtConfig}

import scala.concurrent.duration._

class PasswordEncryptorSpec extends CatsIOSpec {

  val authConfig = AuthConfig(
    "$2a$10$8K1p/a0dL1LXMIgoEDFrwO",
    UserJwtConfig("user-secret-key", 30.minutes),
    AdminJwtConfig("admin-secret-key", "admin-token", "admin-claim")
  )

  "A PasswordEncrypter should" - {

    "hash and validate password with salt" in {
      val result = for {
        e       <- PasswordEncryptor.make[IO](authConfig)
        hash    <- e.hash(User.Password("Password123!"))
        isValid <- e.isValid(User.Password("Password123!"), hash)
      } yield isValid

      result.asserting { isValid =>
        isValid mustBe true
      }
    }
  }
}
