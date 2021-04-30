package io.kirill.shoppingcart

import cats.effect.{IO, Resource}
import com.github.sebruck.EmbeddedRedis
import dev.profunktor.redis4cats.log4cats._
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext

trait RedisSpec extends CatsIOSpec with EmbeddedRedis {
  implicit val ec                 = ExecutionContext.global
  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def stringCommands(port: Int): Resource[IO, RedisCommands[IO, String, String]] =
    Redis[IO].utf8(s"redis://localhost:${port}")
}
