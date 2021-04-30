package io.kirill.shoppingcart.auth.user

import cats.effect.IO
import dev.profunktor.auth.jwt.JwtToken
import io.kirill.shoppingcart.RedisSpec
import io.kirill.shoppingcart.config.{AdminJwtConfig, AuthConfig, UserJwtConfig}

import scala.concurrent.duration._

class UserCacheStoreSpec extends RedisSpec {

  val testUser = UserBuilder.user()

  val authConfig = AuthConfig(
    "$2a$10$8K1p/a0dL1LXMIgoEDFrwO",
    UserJwtConfig("user-secret-key", 30.minutes),
    AdminJwtConfig("admin-secret-key", "admin-token", "admin-claim")
  )

  "A RedisUserCacheStore" - {

    "store user in cache" in {
      withRedisCommands { redis =>
        val result = for {
          store <- UserCacheStore.redisUserCacheStore[IO](redis, authConfig)
          _     <- store.put(JwtToken("token"), testUser)
          token <- store.findToken(testUser.name)
        } yield token

        result.asserting(_ mustBe Some(JwtToken("token")))
      }
    }

    "return user from cache if it exists" in {
      withRedisCommands { redis =>
        val result = for {
          store <- UserCacheStore.redisUserCacheStore[IO](redis, authConfig)
          _     <- store.put(JwtToken("token"), testUser)
          user  <- store.findUser(JwtToken("token"))
        } yield user

        result.asserting(_ mustBe Some(testUser))
      }
    }

    "return empty option if user not found" in {
      withRedisCommands { redis =>
        val result = for {
          store <- UserCacheStore.redisUserCacheStore[IO](redis, authConfig)
          user  <- store.findUser(JwtToken("another-token"))
        } yield user

        result.asserting(_ mustBe None)
      }
    }

    "remove user and token from cache" in {
      withRedisCommands { redis =>
        val result = for {
          store <- UserCacheStore.redisUserCacheStore[IO](redis, authConfig)
          _     <- store.put(JwtToken("token"), testUser)
          _     <- store.remove(JwtToken("token"), testUser.name)
          t     <- store.findToken(testUser.name)
          u     <- store.findUser(JwtToken("token"))
        } yield (t, u)

        result.asserting(_ mustBe ((None, None)))
      }
    }
  }
}
