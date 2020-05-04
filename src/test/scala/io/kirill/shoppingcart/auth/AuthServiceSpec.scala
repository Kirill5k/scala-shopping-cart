package io.kirill.shoppingcart.auth

import cats.effect.IO
import dev.profunktor.auth.jwt.JwtToken
import io.kirill.shoppingcart.auth.user.{Password, PasswordHash, User, UserBuilder, UserCacheStore, UserId, UserRepository, Username}
import io.kirill.shoppingcart.common.errors._
import org.mockito.scalatest.AsyncMockitoSugar
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers

class AuthServiceSpec extends AsyncFreeSpec with Matchers with AsyncMockitoSugar {

  val user = UserBuilder.user()
  val token = JwtToken("token")

  "An AuthService" - {
    "login" - {
      "generate new token and store it in cache" in {
        val (repo, cache, tokenGen, passEncr) = mocks
        val result = for {
          service <- AuthService.make(repo, cache, tokenGen, passEncr)
          _ = when(repo.findByName(any[Username])).thenReturn(IO.pure(Some(user)))
          _ = when(passEncr.isValid(any[Password], any[PasswordHash])).thenReturn(IO.pure(true))
          _ = when(cache.findToken(any[Username])).thenReturn(IO.pure(None))
          _ = when(tokenGen.generate).thenReturn(IO.pure(token))
          _ = when(cache.put(any[JwtToken], any[User])).thenReturn(IO.pure(()))
          res <- service.login(user.name, Password("password"))
        } yield res

        result.unsafeToFuture().map { r =>
          verify(repo).findByName(user.name)
          verify(passEncr).isValid(Password("password"), user.password)
          verify(cache).findToken(user.name)
          verify(tokenGen).generate
          verify(cache).put(token, user)
          r must be (token)
        }
      }

      "return token from cache if password matches and token exists" in {
        val (repo, cache, tokenGen, passEncr) = mocks
        val result = for {
          service <- AuthService.make(repo, cache, tokenGen, passEncr)
          _ = when(repo.findByName(any[Username])).thenReturn(IO.pure(Some(user)))
          _ = when(passEncr.isValid(any[Password], any[PasswordHash])).thenReturn(IO.pure(true))
          _ = when(cache.findToken(any[Username])).thenReturn(IO.pure(Some(token)))
          res <- service.login(user.name, Password("password"))
        } yield res

        result.unsafeToFuture().map { r =>
          verify(repo).findByName(user.name)
          verify(passEncr).isValid(Password("password"), user.password)
          verify(cache).findToken(user.name)
          r must be (token)
        }
      }

      "return InvalidUsernameOrPassword error when password is invalid" in {
        val (repo, cache, tokenGen, passEncr) = mocks
        val result = for {
          service <- AuthService.make(repo, cache, tokenGen, passEncr)
          _ = when(repo.findByName(any[Username])).thenReturn(IO.pure(Some(user)))
          _ = when(passEncr.isValid(any[Password], any[PasswordHash])).thenReturn(IO.pure(false))
          res <- service.login(user.name, Password("password"))
        } yield res

        recoverToSucceededIf[AppError] {
          result.unsafeToFuture()
        }
      }

      "return InvalidUsernameOrPassword error when user not found" in {
        val (repo, cache, tokenGen, passEncr) = mocks
        val result = for {
          service <- AuthService.make(repo, cache, tokenGen, passEncr)
          _ = when(repo.findByName(any[Username])).thenReturn(IO.pure(None))
          res <- service.login(user.name, Password("password"))
        } yield res

        recoverToSucceededIf[AppError] {
          result.unsafeToFuture()
        }
      }
    }
  }

  def mocks: (UserRepository[IO], UserCacheStore[IO], TokenGenerator[IO], PasswordEncryptor[IO]) =
    (mock[UserRepository[IO]], mock[UserCacheStore[IO]], mock[TokenGenerator[IO]], mock[PasswordEncryptor[IO]])
}
