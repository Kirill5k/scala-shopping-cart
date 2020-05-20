package io.kirill.shoppingcart

import cats.effect.{ExitCode, IO, IOApp}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.kirill.shoppingcart.auth.Auth
import io.kirill.shoppingcart.health.Health
import io.kirill.shoppingcart.shop.Shop
import org.http4s.server.blaze.BlazeServerBuilder

object Application extends IOApp {
  import config.AppConfig._
  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    Resources.make[IO].use { res =>
      for {
        _      <- logger.info("starting scala-shopping-cart app...")
        health <- Health.make[IO](res)
        auth   <- Auth.make[IO](res)
        shop   <- Shop.make[IO](res)
        http   <- Http.make[IO](auth, health, shop)
        _ <- BlazeServerBuilder[IO]
          .bindHttp(appConfig.server.port, appConfig.server.host)
          .withHttpApp(http.httpApp)
          .serve
          .compile
          .drain
        _ <- logger.info("scala-shopping-cart has started!")
      } yield ExitCode.Success
    }
}
