package io.kirill.shoppingcart.auth

import java.util.UUID

import cats.effect.IO
import cats.implicits._
import dev.profunktor.auth.jwt.{JwtAuth, JwtToken}
import io.kirill.shoppingcart.auth.user.{User, UserBuilder, UserCacheStore}
import org.mockito.scalatest.AsyncMockitoSugar
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import pdi.jwt.{JwtAlgorithm, JwtClaim}

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

      result.unsafeToFuture().map(_ must be(Some(CommonUser(testUser))))
    }
  }

  "A AdminUserAuthenticator" - {
    val adminJwtAuth: AdminJwtAuth = AdminJwtAuth(JwtAuth.hmac("admin-secret-key", JwtAlgorithm.HS256))
    val adminToken: JwtToken = JwtToken(
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWQiOiJjNTUyYWI0Ni05NmUxLTExZWEtYmIzNy0wMjQyYWMxMzAwMDIiLCJpYXQiOjE1MTYyMzkwMjJ9.aU-fe4RCRvzwnQfEVj0w-Ycmv3FxuV_FiR5YEz9SS1Y"
    )

    "should return admin user if token matches" in {
      val result = for {
        adminAuth <- Authenticator.adminUserAuthenticator[IO](adminToken, adminJwtAuth)
        res       <- adminAuth.findUser(adminToken)(mock[JwtClaim])
      } yield res

      result
        .unsafeToFuture()
        .map(_ must be(Some(AdminUser(User(User.Id(UUID.fromString("c552ab46-96e1-11ea-bb37-0242ac130002")), User.Name("admin"), None)))))
    }
  }
}
