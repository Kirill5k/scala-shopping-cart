package io.kirill.shoppingcart.auth.user

import java.util.UUID

import cats.effect.IO
import dev.profunktor.auth.jwt.JwtToken
import io.kirill.shoppingcart.CatsIOSpec
import io.kirill.shoppingcart.auth.AdminUser
import pdi.jwt.JwtClaim

class UserStoreSpec extends CatsIOSpec {

  val adminToken = JwtToken("admin-token")
  val adminUser = AdminUser(User(UserId(UUID.randomUUID()), Username("Admin"), PasswordHash("hash")))

  "An AdminUserStore" - {

    "return admin user is admin jwt token passed" in {
      val result = for {
        store <- UserStore.adminUserStore[IO](adminToken, adminUser)
        au <- store.findUser(JwtToken("admin-token"))(JwtClaim())
      } yield au

      result.asserting(_ must be (Some(adminUser)))
    }

    "return empty option in token is not admin" in {
      val result = for {
        store <- UserStore.adminUserStore[IO](adminToken, adminUser)
        au <- store.findUser(JwtToken("standard-token"))(JwtClaim())
      } yield au

      result.asserting(_ must be (None))
    }
  }

  "A CommonUserStore" - {

  }
}
