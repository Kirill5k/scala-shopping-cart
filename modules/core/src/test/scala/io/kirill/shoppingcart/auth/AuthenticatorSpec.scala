package io.kirill.shoppingcart.auth

import cats.effect.IO
import cats.implicits._
import dev.profunktor.auth.jwt.JwtToken
import io.kirill.shoppingcart.auth.user.{UserBuilder, UserCacheStore}
import org.mockito.scalatest.AsyncMockitoSugar
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import pdi.jwt.JwtClaim

class AuthenticatorSpec extends AsyncFreeSpec with Matchers with AsyncMockitoSugar {

  "A CommonUserAuthenticator" - {

    val testUser = UserBuilder.user()

    "should find user in cache" in {
      val cache = mock[UserCacheStore[IO]]
      val result = for {
        userAuth <- Authenticator.commonUserAuthenticator(cache)
        _ = when(cache.findUser(any[JwtToken])).thenReturn(IO.pure(Some(testUser)))
        res <- userAuth.findUser(JwtToken("token"))(mock[JwtClaim])
      } yield res

      result.unsafeToFuture().map(_ must be (Some(CommonUser(testUser))))
    }
  }

  "A AdminUserAuthenticator" - {}
}
