package io.kirill.shoppingcart.auth

import cats.effect.Sync
import cats.implicits._
import io.circe.generic.auto._
import io.kirill.shoppingcart.common.web.RestController
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.HttpRoutes
import org.http4s.server.Router
import eu.timepit.refined.api.Refined
import eu.timepit.refined.generic._
import eu.timepit.refined.collection._
import eu.timepit.refined.string._
import io.circe.refined._

final class AuthController[F[_]: Sync](authService: AuthService[F]) extends RestController[F]{
  import RestController._
  import AuthController._

  private val prefixPath = "/auth"

  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" => withErrorHandling {
      for {
        login <- req.decodeR[AuthLoginRequest]
        token <- authService.login(Username(login.username.value), Password(login.password.value))
        res <- Ok(AuthLoginResponse(token.value))
      } yield res
    }
  }

  val routes: HttpRoutes[F] =
    Router(prefixPath -> loginRoute)
}

object AuthController {
  type NonEmptyString = String Refined NonEmpty

  final case class AuthLoginRequest(username: NonEmptyString, password: NonEmptyString)

  final case class AuthLoginResponse(token: String)
}
