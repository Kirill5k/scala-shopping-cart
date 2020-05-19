package io.kirill.shoppingcart.auth.user

import cats.effect.IO
import io.kirill.shoppingcart.PostgresRepositorySpec
import io.kirill.shoppingcart.common.errors.UniqueViolation

class UserRepositorySpec extends PostgresRepositorySpec {

  "A UserRepository" - {

    "create new user and find it" in {
      val repository = UserRepository.make[IO](session)

      val result = for {
        r    <- repository
        uid  <- r.create(Username("boris"), PasswordHash("password"))
        user <- r.findByName(Username("boris"))
      } yield (uid, user.get)

      result.asserting {
        case (uid, user) =>
          user.id must be(uid)
          user.name must be(Username("boris"))
          user.password must be(Some(PasswordHash("password")))
      }
    }

    "return error if username is taken" in {
      val repository = UserRepository.make[IO](session)

      val result = for {
        r <- repository
        _ <- r.create(Username("boris"), PasswordHash("password"))
        _ <- r.create(Username("boris"), PasswordHash("password"))
      } yield ()

      result.assertThrows[UniqueViolation]
    }
  }
}
