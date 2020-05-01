package io.kirill.shoppingcart.auth

import cats.effect.IO
import io.kirill.shoppingcart.CatsIOSpec
import io.kirill.shoppingcart.auth.user.Password

class PasswordEncryptorSpec extends CatsIOSpec {

  "A basic PasswordEncrypter" - {

    "hash and validate password with salt" in {
      val secret = PasswordSalt("$2a$10$8K1p/a0dL1LXMIgoEDFrwO")
      val result = for {
        e <- IO(PasswordEncryptor[IO](Some(secret)))
        hash <- e.hash(Password("Password123!"))
        isValid <- e.isValid(Password("Password123!"), hash)
      } yield isValid

      result.asserting { isValid =>
        isValid must be (true)
      }
    }

    "hash and validate password without salt" in {
      val result = for {
        e <- IO(PasswordEncryptor[IO](None))
        hash <- e.hash(Password("Password123!"))
        isValid <- e.isValid(Password("Password123!"), hash)
        isNotValid <- e.isValid(Password("foo-Password123!"), hash)
      } yield (isValid, isNotValid)

      result.asserting { case (isValid, isNotValid) =>
        isValid must be (true)
        isNotValid must be (false)
      }
    }
  }
}
