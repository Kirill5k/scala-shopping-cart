package io.kirill.shoppingcart

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.kirill.shoppingcart.auth.Auth
import io.kirill.shoppingcart.config.AppConfig
import io.kirill.shoppingcart.health.Health
import io.kirill.shoppingcart.shop.Shop
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Application extends IOApp {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    Blocker[IO].use(AppConfig.load[IO]).flatMap { config =>
      Resources.make[IO](config, ExecutionContext.global).use { res =>
        for {
          _      <- logger.info("starting scala-shopping-cart app...")
          health <- Health.make[IO](res)
          auth   <- Auth.make[IO](res, config.auth)
          shop   <- Shop.make[IO](res, config.shop)
          http   <- Http.make[IO](auth, health, shop)
          _ <- BlazeServerBuilder[IO](ExecutionContext.global)
            .bindHttp(config.server.port, config.server.host)
            .withHttpApp(http.httpApp)
            .serve
            .compile
            .drain
        } yield ()
      }
    }
  }.as(ExitCode.Success)
}
