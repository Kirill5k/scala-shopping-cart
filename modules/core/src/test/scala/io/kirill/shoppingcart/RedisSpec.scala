package io.kirill.shoppingcart

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{IO, Resource}
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import com.github.sebruck.EmbeddedRedis
import dev.profunktor.redis4cats.algebra.RedisCommands
import dev.profunktor.redis4cats.connection.{RedisClient, RedisURI}
import dev.profunktor.redis4cats.domain.RedisCodec
import dev.profunktor.redis4cats.interpreter.Redis
import dev.profunktor.redis4cats.log4cats._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

trait RedisSpec extends CatsIOSpec with EmbeddedRedis {
  implicit val ec                 = scala.concurrent.ExecutionContext.global
  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def stringCommands(port: Int): Resource[IO, RedisCommands[IO, String, String]] =
    for {
      uri    <- Resource.liftF(RedisURI.make[IO](s"redis://localhost:$port"))
      client <- RedisClient[IO](uri)
      redis  <- Redis[IO, String, String](client, RedisCodec.Utf8)
    } yield redis
}
