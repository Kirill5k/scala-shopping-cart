package io.kirill.shoppingcart.auth.user

import java.util.UUID

import cats.effect.{IO, Resource}
import com.github.sebruck.EmbeddedRedis
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.algebra.{RedisCommands, StringCommands}
import dev.profunktor.redis4cats.connection.{RedisClient, RedisURI}
import dev.profunktor.redis4cats.domain.RedisCodec
import dev.profunktor.redis4cats.interpreter.Redis
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import dev.profunktor.redis4cats.log4cats._
import io.kirill.shoppingcart.CatsIOSpec
import io.kirill.shoppingcart.auth.{AdminUser, CommonUser}
import pdi.jwt.JwtClaim

class UserStoreSpec extends CatsIOSpec with EmbeddedRedis {
  implicit val ec = scala.concurrent.ExecutionContext.global
  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val adminToken = JwtToken("admin-token")
  val adminUser  = AdminUser(User(UserId(UUID.randomUUID()), Username("Admin"), PasswordHash("hash")))

  "An AdminUserStore" - {

    "return admin user is admin jwt token passed" in {
      val result = for {
        store <- UserStore.adminUserStore[IO](adminToken, adminUser)
        au    <- store.findUser(JwtToken("admin-token"))(JwtClaim())
      } yield au

      result.asserting(_ must be(Some(adminUser)))
    }

    "return empty option in token is not admin" in {
      val result = for {
        store <- UserStore.adminUserStore[IO](adminToken, adminUser)
        au    <- store.findUser(JwtToken("standard-token"))(JwtClaim())
      } yield au

      result.asserting(_ must be(None))
    }
  }

  "A CommonUserStore" - {
    "return common user from cache if it exists" in {
      withRedisAsync() { port =>
        val result = for {
          store <- UserStore.commonUserStore[IO](commandsApi(port))
          user <- store.findUser(JwtToken("jwt-token"))(JwtClaim())
        } yield user

        result.asserting(_ must be (Some(CommonUser(User(UserId(UUID.fromString("722ccbba-8c62-11ea-bc55-0242ac130003")), Username("Boris"), PasswordHash("password-hash"))))))
      }
    }

    "return empty option if user not found" in {
      withRedisAsync() { port =>
        val result = for {
          store <- UserStore.commonUserStore[IO](commandsApi(port))
          user <- store.findUser(JwtToken("another-token"))(JwtClaim())
        } yield user

        result.asserting(_ must be (None))
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
