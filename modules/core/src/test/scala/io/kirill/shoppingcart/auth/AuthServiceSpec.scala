package io.kirill.shoppingcart.auth

import cats.effect.IO
import dev.profunktor.auth.jwt.JwtToken
import io.kirill.shoppingcart.auth.user.{User, UserBuilder, UserCacheStore, UserRepository}
import io.kirill.shoppingcart.common.errors._
import org.mockito.scalatest.AsyncMockitoSugar
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers

class AuthServiceSpec extends AsyncFreeSpec with Matchers with AsyncMockitoSugar {

  val user  = UserBuilder.user()
  val token = JwtToken("token")

  "An AuthService when" - {
    "create should" - {
      "return user id" in {
        val (repo, cache, tokenGen, passEncr) = mocks
        when(passEncr.hash(any[User.Password])).thenReturn(IO.pure(user.password.get))
        when(repo.create(any[User.Name], any[User.PasswordHash])).thenReturn(IO.pure(user.id))

        val result = for {
          service <- AuthService.make(repo, cache, tokenGen, passEncr)
          res <- service.create(user.name, User.Password("password"))
        } yield res

        result.unsafeToFuture().map { r =>
          verify(repo).create(user.name, user.password.get)
          verify(passEncr).hash(User.Password("password"))
          r must be(user.id)
        }
      }

      "return error if user with such name already exists" in {
        val (repo, cache, tokenGen, passEncr) = mocks
        when(repo.create(any[User.Name], any[User.PasswordHash])).thenReturn(IO.raiseError(UniqueViolation("name already taken")))
        when(passEncr.hash(any[User.Password])).thenReturn(IO.pure(user.password.get))

        val result = for {
          service <- AuthService.make(repo, cache, tokenGen, passEncr)
          res <- service.create(user.name, User.Password("password"))
        } yield res

        recoverToSucceededIf[UsernameInUse] {
          result.unsafeToFuture()
        }
      }
    }

    "logout should" - {
      "clear cache" in {
        val (repo, cache, tokenGen, passEncr) = mocks
        when(cache.remove(any[JwtToken], any[User.Name])).thenReturn(IO.unit)

        val result = for {
          service <- AuthService.make(repo, cache, tokenGen, passEncr)
          res <- service.logout(token, user.name)
        } yield res

        result.unsafeToFuture().map { r =>
          verify(cache).remove(token, user.name)
          r must be(())
        }
      }
    }

    "login should" - {
      "generate new token and store it in cache" in {
        val (repo, cache, tokenGen, passEncr) = mocks
        when(repo.findByName(any[User.Name])).thenReturn(IO.pure(Some(user)))
        when(passEncr.isValid(any[User.Password], any[User.PasswordHash])).thenReturn(IO.pure(true))
        when(cache.findToken(any[User.Name])).thenReturn(IO.pure(None))
        when(tokenGen.generate).thenReturn(IO.pure(token))
        when(cache.put(any[JwtToken], any[User])).thenReturn(IO.unit)

        val result = for {
          service <- AuthService.make(repo, cache, tokenGen, passEncr)
          res     <- service.login(user.name, User.Password("password"))
        } yield res

        result.unsafeToFuture().map { r =>
          verify(repo).findByName(user.name)
          verify(passEncr).isValid(User.Password("password"), user.password.get)
          verify(cache).findToken(user.name)
          verify(tokenGen).generate
          verify(cache).put(token, user)
          r mustBe token
        }
      }

      "return token from cache if password matches and token exists" in {
        val (repo, cache, tokenGen, passEncr) = mocks
        when(repo.findByName(any[User.Name])).thenReturn(IO.pure(Some(user)))
        when(passEncr.isValid(any[User.Password], any[User.PasswordHash])).thenReturn(IO.pure(true))
        when(cache.findToken(any[User.Name])).thenReturn(IO.pure(Some(token)))

        val result = for {
          service <- AuthService.make(repo, cache, tokenGen, passEncr)
          res <- service.login(user.name, User.Password("password"))
        } yield res

        result.unsafeToFuture().map { r =>
          verify(repo).findByName(user.name)
          verify(passEncr).isValid(User.Password("password"), user.password.get)
          verify(cache).findToken(user.name)
          r must be(token)
        }
      }

      "return InvalidUsernameOrPassword error when password is invalid" in {
        val (repo, cache, tokenGen, passEncr) = mocks
        when(repo.findByName(any[User.Name])).thenReturn(IO.pure(Some(user)))
        when(passEncr.isValid(any[User.Password], any[User.PasswordHash])).thenReturn(IO.pure(false))

        val result = for {
          service <- AuthService.make(repo, cache, tokenGen, passEncr)
          res <- service.login(user.name, User.Password("password"))
        } yield res

        recoverToSucceededIf[InvalidUsernameOrPassword] {
          result.unsafeToFuture()
        }
      }

      "return InvalidUsernameOrPassword when user has no password set" in {
        val (repo, cache, tokenGen, passEncr) = mocks
        when(repo.findByName(any[User.Name])).thenReturn(IO.pure(Some(user.copy(password = None))))

        val result = for {
          service <- AuthService.make(repo, cache, tokenGen, passEncr)
          res <- service.login(user.name, User.Password("password"))
        } yield res

        recoverToSucceededIf[InvalidUsernameOrPassword] {
          result.unsafeToFuture()
        }
      }

      "return InvalidUsernameOrPassword error when user not found" in {
        val (repo, cache, tokenGen, passEncr) = mocks
        when(repo.findByName(any[User.Name])).thenReturn(IO.pure(None))

        val result = for {
          service <- AuthService.make(repo, cache, tokenGen, passEncr)
          res <- service.login(user.name, User.Password("password"))
        } yield res

        recoverToSucceededIf[InvalidUsernameOrPassword] {
          result.unsafeToFuture()
        }
      }
    }
  }

  def mocks: (UserRepository[IO], UserCacheStore[IO], TokenGenerator[IO], PasswordEncryptor[IO]) =
    (mock[UserRepository[IO]], mock[UserCacheStore[IO]], mock[TokenGenerator[IO]], mock[PasswordEncryptor[IO]])
}
