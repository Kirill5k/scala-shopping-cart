package io.kirill.shoppingcart

import java.util.concurrent.TimeUnit

import cats.effect.{Concurrent, Sync, Timer}
import cats.implicits._
import io.kirill.shoppingcart.auth.Auth
import io.kirill.shoppingcart.health.Health
import io.kirill.shoppingcart.shop.Shop
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware._

import scala.concurrent.duration.FiniteDuration

final class Http[F[_]: Concurrent: Timer] private (
    auth: Auth[F],
    health: Health[F],
    shop: Shop[F]
) {

  private val routes: HttpRoutes[F] =
    auth.authController.routes(auth.userMiddleware) <+>
      health.healthCheckController.routes <+>
      shop.routes(auth.userMiddleware, auth.adminMiddleware)

  private val middleware: HttpRoutes[F] => HttpRoutes[F] = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    } andThen { http: HttpRoutes[F] =>
      CORS(http, CORS.DefaultCORSConfig)
    } andThen { http: HttpRoutes[F] =>
      Timeout(FiniteDuration(60, TimeUnit.SECONDS))(http)
    }
  }

  private val loggers: HttpApp[F] => HttpApp[F] = {
    { http: HttpApp[F] =>
      RequestLogger.httpApp(true, true)(http)
    } andThen { http: HttpApp[F] =>
      ResponseLogger.httpApp(true, true)(http)
    }
  }

  val httpApp: HttpApp[F] = loggers(middleware(routes).orNotFound)
}

object Http {
  def make[F[_]: Concurrent: Timer](
      auth: Auth[F],
      health: Health[F],
      shop: Shop[F]
  ): F[Http[F]] = Sync[F].delay(new Http(auth, health, shop))
}
