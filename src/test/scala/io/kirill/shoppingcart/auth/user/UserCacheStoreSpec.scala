package io.kirill.shoppingcart.auth.user

import java.util.UUID

import cats.effect.{IO, Resource}
import com.github.sebruck.EmbeddedRedis
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.algebra.RedisCommands
import dev.profunktor.redis4cats.connection.{RedisClient, RedisURI}
import dev.profunktor.redis4cats.domain.RedisCodec
import dev.profunktor.redis4cats.interpreter.Redis
import dev.profunktor.redis4cats.log4cats._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.kirill.shoppingcart.CatsIOSpec


class UserCacheStoreSpec extends CatsIOSpec with EmbeddedRedis {
  implicit val ec = scala.concurrent.ExecutionContext.global
  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  import io.kirill.shoppingcart.config.AppConfig.appConfig

  val testUser = User(UserId(UUID.randomUUID()), Username("Boris"), PasswordHash("password-hash"))

  "A RedisUserCacheStore" - {

    "store user in cache" in {
      withRedisAsync() { port =>
        val result = for {
          store <- UserStore.redisUserCacheStore[IO](commandsApi(port))
          _ <- store.put(JwtToken("token"), testUser)
          token <- store.findToken(testUser.name)
        } yield token

        result.asserting(_ must be (Some(JwtToken("token"))))
      }
    }

    "return user from cache if it exists" in {
      withRedisAsync() { port =>
        val result = for {
          store <- UserStore.redisUserCacheStore[IO](commandsApi(port))
          user <- store.findUser(JwtToken("jwt-token"))
        } yield user

        result.asserting(_ must be (Some(User(UserId(UUID.fromString("722ccbba-8c62-11ea-bc55-0242ac130003")), Username("Boris"), PasswordHash("password-hash")))))
      }
    }

    "return empty option if user not found" in {
      withRedisAsync() { port =>
        val result = for {
          store <- UserStore.redisUserCacheStore[IO](commandsApi(port))
          user <- store.findUser(JwtToken("another-token"))
        } yield user

        result.asserting(_ must be (None))
      }
    }

    "remove user and token from cache" in {
      withRedisAsync() { port =>
        val result = for {
          store <- UserStore.redisUserCacheStore[IO](commandsApi(port))
          _ <- store.put(JwtToken("token"), testUser)
          _ <- store.remove(JwtToken("token"), testUser.name)
          t <- store.findToken(testUser.name)
          u <- store.findUser(JwtToken("token"))
        } yield (t, u)

        result.asserting(_ must be ((None, None)))
      }
    }
  }

  def commandsApi(port: Int): Resource[IO, RedisCommands[IO, String, String]] =
    for {
      uri    <- Resource.liftF(RedisURI.make[IO](s"redis://localhost:$port"))
      client <- RedisClient[IO](uri)
      redis  <- Redis[IO, String, String](client, RedisCodec.Utf8)
      _ <- Resource.liftF(redis.set("jwt-token", """{"id": "722ccbba-8c62-11ea-bc55-0242ac130003", "name": "Boris", "password": "password-hash"}"""))
    } yield redis
}
