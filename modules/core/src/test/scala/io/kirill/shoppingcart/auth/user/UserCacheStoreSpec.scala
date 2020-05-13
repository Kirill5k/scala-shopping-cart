package io.kirill.shoppingcart.auth.user

import java.util.UUID

import cats.effect.IO
import dev.profunktor.auth.jwt.JwtToken
import io.kirill.shoppingcart.{RedisSpec}


class UserCacheStoreSpec extends RedisSpec {

  import io.kirill.shoppingcart.config.AppConfig.appConfig

  val testUser = User(UserId(UUID.randomUUID()), Username("Boris"), PasswordHash("password-hash"))

  "A RedisUserCacheStore" - {

    "store user in cache" in {
      withRedisAsync() { port =>
        val result = stringCommands(port).use(r => for {
          store <- UserStore.redisUserCacheStore[IO](r)
          _ <- store.put(JwtToken("token"), testUser)
          token <- store.findToken(testUser.name)
        } yield token)

        result.asserting(_ must be (Some(JwtToken("token"))))
      }
    }

    "return user from cache if it exists" in {
      withRedisAsync() { port =>
        val result = stringCommands(port).use(r => for {
          store <- UserStore.redisUserCacheStore[IO](r)
          _ <- store.put(JwtToken("token"), testUser)
          user <- store.findUser(JwtToken("token"))
        } yield user)

        result.asserting(_ must be (Some(testUser)))
      }
    }

    "return empty option if user not found" in {
      withRedisAsync() { port =>
        val result = stringCommands(port).use(r => for {
          store <- UserStore.redisUserCacheStore[IO](r)
          user <- store.findUser(JwtToken("another-token"))
        } yield user)

        result.asserting(_ must be (None))
      }
    }

    "remove user and token from cache" in {
      withRedisAsync() { port =>
        val result = stringCommands(port).use(r => for {
          store <- UserStore.redisUserCacheStore[IO](r)
          _ <- store.put(JwtToken("token"), testUser)
          _ <- store.remove(JwtToken("token"), testUser.name)
          t <- store.findToken(testUser.name)
          u <- store.findUser(JwtToken("token"))
        } yield (t, u))

        result.asserting(_ must be ((None, None)))
      }
    }
  }
}
