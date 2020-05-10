package io.kirill.shoppingcart.auth


import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.AuthHeaders
import dev.profunktor.auth.jwt.JwtToken
import io.circe.generic.auto._
import io.kirill.shoppingcart.common.json._
import io.kirill.shoppingcart.common.web.RestController
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.server.{AuthMiddleware, Router}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection._
import io.chrisdavenport.log4cats.Logger
import io.circe.refined._
import io.kirill.shoppingcart.auth.user.{Password, UserId, Username}
import io.kirill.shoppingcart.common.errors.AuthTokenNotPresent

final class AuthController[F[_]: Sync: Logger](authService: AuthService[F]) extends RestController[F]{
  import RestController._
  import AuthController._

  private val prefixPath = "/users"

  private val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root => withErrorHandling {
      for {
        create <- req.decodeR[AuthCreateUserRequest]
        uid <- authService.create(Username(create.username.value), Password(create.password.value))
        res <- Created(AuthCreateUserResponse(uid))
      } yield res
    }
    case req @ POST -> Root / "auth" / "login" => withErrorHandling {
      for {
        login <- req.decodeR[AuthLoginRequest]
        token <- authService.login(Username(login.username.value), Password(login.password.value))
        res <- Ok(AuthLoginResponse(token))
      } yield res
    }
  }

  private val authedRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    case authedReq @ POST -> Root / "auth" / "logout" as user => withErrorHandling {
      AuthHeaders.getBearerToken(authedReq.req) match {
        case Some(token) => authService.logout(token, user.value.name) *> NoContent()
        case None => Sync[F].raiseError(AuthTokenNotPresent(user.value.name))
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

  final case class AuthCreateUserResponse(id: UserId)

  final case class AuthLoginRequest(username: NonEmptyString, password: NonEmptyString)

  final case class AuthLoginResponse(token: JwtToken)
}
