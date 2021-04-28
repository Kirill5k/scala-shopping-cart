package io.kirill.shoppingcart.auth

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.AuthHeaders
import dev.profunktor.auth.jwt.JwtToken
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.auto._
import io.kirill.shoppingcart.auth.user.User
import io.kirill.shoppingcart.common.errors.AuthTokenNotPresent
import io.kirill.shoppingcart.common.web.RestController
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.typelevel.log4cats.Logger

final class AuthController[F[_]: Sync: Logger](authService: AuthService[F]) extends RestController[F] {
  import AuthController._

  private val prefixPath = "/users"

  private val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root =>
      withErrorHandling {
        for {
          create <- req.as[AuthCreateUserRequest]
          uid    <- authService.create(User.Name(create.username.value), User.Password(create.password.value))
          res    <- Created(AuthCreateUserResponse(uid))
        } yield res
      }
    case req @ POST -> Root / "auth" / "login" =>
      withErrorHandling {
        for {
          login <- req.as[AuthLoginRequest]
          token <- authService.login(User.Name(login.username.value), User.Password(login.password.value))
          res   <- Ok(AuthLoginResponse(token))
        } yield res
      }
  }

  private val authedRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    case authedReq @ POST -> Root / "auth" / "logout" as user =>
      withErrorHandling {
        AuthHeaders.getBearerToken(authedReq.req) match {
          case Some(token) => authService.logout(token, user.value.name) *> NoContent()
          case None        => Sync[F].raiseError(AuthTokenNotPresent(user.value.name))
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
  final case class AuthCreateUserRequest(username: NonEmptyString, password: NonEmptyString)
  final case class AuthCreateUserResponse(id: User.Id)
  final case class AuthLoginRequest(username: NonEmptyString, password: NonEmptyString)
  final case class AuthLoginResponse(token: JwtToken)

  def make[F[_]: Sync: Logger](authService: AuthService[F]): F[AuthController[F]] =
    Sync[F].delay(new AuthController[F](authService))
}
