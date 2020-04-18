package io.kirill.shoppingcart.auth

import cats.effect.Sync
import cats.implicits._
import io.circe.generic.auto._
import io.kirill.shoppingcart.common.web.RestController
import io.kirill.shoppingcart.common.web.json._
import org.http4s.HttpRoutes
import org.http4s.server.Router

final class AuthController[F[_]: Sync](authService: AuthService[F]) extends RestController[F]{
  import AuthController._

  private val prefixPath = "/auth"

  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" => withErrorHandling {
      for {
        login <- req.as[AuthLoginRequest]
        token <- authService.login(Username(login.username), Password(login.password))
        res <- Ok(AuthLoginResponse(token.value))
      } yield res
    }
  }

  val routes: HttpRoutes[F] =
    Router(prefixPath -> loginRoute)
}

object AuthController {
  final case class AuthLoginRequest(username: String, password: String)

  final case class AuthLoginResponse(token: String)
}
