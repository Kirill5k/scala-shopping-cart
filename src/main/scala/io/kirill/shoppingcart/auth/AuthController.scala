package io.kirill.shoppingcart.auth

import java.util.UUID

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.AuthHeaders
import io.circe.generic.auto._
import io.kirill.shoppingcart.common.web.RestController
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.server.{AuthMiddleware, Router}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection._
import io.circe.refined._
import io.kirill.shoppingcart.auth.user.{Password, Username}
import io.kirill.shoppingcart.common.errors.AuthTokenNotPresent

final class AuthController[F[_]: Sync](authService: AuthService[F]) extends RestController[F]{
  import RestController._
  import AuthController._

  private val prefixPath = "/auth"

  private val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" => withErrorHandling {
      for {
        create <- req.decodeR[AuthCreateUserRequest]
        userId <- authService.create(Username(create.username.value), Password(create.password.value))
        res <- Ok(AuthCreateUserResponse(userId.value))
      } yield res
    }
    case req @ POST -> Root / "login" => withErrorHandling {
      for {
        login <- req.decodeR[AuthLoginRequest]
        token <- authService.login(Username(login.username.value), Password(login.password.value))
        res <- Ok(AuthLoginResponse(token.value))
      } yield res
    }
  }

  private val authedRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    case authedReq @ POST -> Root / "logout" as user => withErrorHandling {
      AuthHeaders.getBearerToken(authedReq.req) match {
        case Some(token) => authService.logout(user.value.name, token) *> NoContent()
        case None => Sync[F].raiseError(AuthTokenNotPresent)
      }
    }
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] =
    Router(
      prefixPath -> authMiddleware(authedRoutes),
      prefixPath -> routes
    )
}

object AuthController {
  type NonEmptyString = String Refined NonEmpty

  final case class AuthCreateUserRequest(username: NonEmptyString, password: NonEmptyString)

  final case class AuthCreateUserResponse(id: UUID)

  final case class AuthLoginRequest(username: NonEmptyString, password: NonEmptyString)

  final case class AuthLoginResponse(token: String)
}
