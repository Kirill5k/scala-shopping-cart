package io.kirill.shoppingcart

import cats.effect.{ExitCode, IO, IOApp}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

object Application extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      logger <- Slf4jLogger.create[IO]
      _ <- logger.info("Hello, World!")
    } yield ExitCode.Success
}
