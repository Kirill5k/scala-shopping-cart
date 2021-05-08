package io.kirill.shoppingcart.auth

import java.util.UUID

import cats.effect.{ContextShift, IO}
import dev.profunktor.auth.jwt.JwtToken
import io.circe.generic.auto._
import io.circe.parser._
import io.kirill.shoppingcart.ControllerSpec
import io.kirill.shoppingcart.auth.AuthController._
import io.kirill.shoppingcart.common.web.json._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.literal._
import io.kirill.shoppingcart.auth.user.User
import io.kirill.shoppingcart.common.errors.{InvalidUsernameOrPassword, UsernameInUse}
import io.kirill.shoppingcart.common.web.ErrorResponse
import org.http4s.circe._
import org.http4s._
import org.http4s.implicits._

import scala.concurrent.ExecutionContext

class AuthControllerSpec extends ControllerSpec {

  val userId = UUID.fromString("d09c402a-8615-11ea-bc55-0242ac130003")

  "An AuthController" should {

    def loginRequestJson(name: String = "boris", password: String = "password"): Json =
      json"""{"username":$name,"password":$password}"""

    def createUserRequestJson(name: String = "boris", password: String = "password"): Json =
      json"""{"username":$name,"password":$password}"""

    "POST /user/auth/logout" should {
      "return no content on success" in {
        val authServiceMock = mock[AuthService[IO]]
        val controller      = new AuthController[IO](authServiceMock)

        when(authServiceMock.logout(any[JwtToken], any[User.Name])).thenReturn(IO.unit)

        val request                    = Request[IO](uri = uri"/users/auth/logout", method = Method.POST).withHeaders(Header("Authorization", "Bearer token"))
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[AuthLoginResponse](response, Status.NoContent, None)
        verify(authServiceMock).logout(JwtToken("token"), User.Name("Boris"))
      }
    }

    "POST /users/auth/login" should {
      "return token on success" in {
        val authServiceMock = mock[AuthService[IO]]
        val controller      = new AuthController[IO](authServiceMock)

        when(authServiceMock.login(any[User.Name], any[User.Password])).thenReturn(IO.pure(JwtToken("token")))

        val request                    = Request[IO](uri = uri"/users/auth/login", method = Method.POST).withEntity(loginRequestJson())
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[AuthLoginResponse](response, Status.Ok, Some(AuthLoginResponse(JwtToken("token"))))
        verify(authServiceMock).login(User.Name("boris"), User.Password("password"))
      }

      "return forbidden when password is incorrect" in {
        val authServiceMock = mock[AuthService[IO]]
        val controller      = new AuthController[IO](authServiceMock)

        when(authServiceMock.login(any[User.Name], any[User.Password]))
          .thenReturn(IO.raiseError(InvalidUsernameOrPassword(authedUser.value.name)))

        val request                    = Request[IO](uri = uri"/users/auth/login", method = Method.POST).withEntity(loginRequestJson())
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](response, Status.Forbidden, Some(ErrorResponse("Username or password is incorrect")))
        verify(authServiceMock).login(User.Name("boris"), User.Password("password"))
      }

      "return bad request when empty login or password" in {
        val authServiceMock = mock[AuthService[IO]]
        val controller      = new AuthController[IO](authServiceMock)

        val request                    = Request[IO](uri = uri"/users/auth/login", method = Method.POST).withEntity(loginRequestJson("", ""))
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](
          response,
          Status.BadRequest,
          Some(ErrorResponse("Predicate isEmpty() did not fail.: DownField(username)"))
        )
        verify(authServiceMock, never).login(any[User.Name], any[User.Password])
      }

      "return unprocessable entity when ivalid json" in {
        val authServiceMock = mock[AuthService[IO]]
        val controller      = new AuthController[IO](authServiceMock)

        val request                    = Request[IO](uri = uri"/users/auth/login", method = Method.POST).withEntity("foo")
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](
          response,
          Status.BadRequest,
          Some(ErrorResponse("""Attempt to decode value on failed cursor: DownField(username)"""))
        )
        verify(authServiceMock, never).login(any[User.Name], any[User.Password])
      }

    }

    "POST /users" should {
      "register new user on success" in {
        val authServiceMock = mock[AuthService[IO]]
        val controller      = new AuthController[IO](authServiceMock)

        when(authServiceMock.create(any[User.Name], any[User.Password])).thenReturn(IO.pure(User.Id(userId)))

        val request                    = Request[IO](uri = uri"/users", method = Method.POST).withEntity(createUserRequestJson())
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[AuthCreateUserResponse](response, Status.Created, Some(AuthCreateUserResponse(User.Id(userId))))
        verify(authServiceMock).create(User.Name("boris"), User.Password("password"))
      }

      "return bad request when username is not provided" in {
        val authServiceMock = mock[AuthService[IO]]
        val controller      = new AuthController[IO](authServiceMock)

        val request                    = Request[IO](uri = uri"/users", method = Method.POST).withEntity(createUserRequestJson(name = ""))
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](
          response,
          Status.BadRequest,
          Some(ErrorResponse("Predicate isEmpty() did not fail.: DownField(username)"))
        )
        verify(authServiceMock, never).create(any[User.Name], any[User.Password])
      }

      "return bad request when username is taken" in {
        val authServiceMock = mock[AuthService[IO]]
        val controller      = new AuthController[IO](authServiceMock)

        when(authServiceMock.create(any[User.Name], any[User.Password])).thenReturn(IO.raiseError(UsernameInUse(User.Name("Boris"))))

        val request                    = Request[IO](uri = uri"/users", method = Method.POST).withEntity(createUserRequestJson())
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](response, Status.BadRequest, Some(ErrorResponse("Username Boris is already taken")))
        verify(authServiceMock).create(User.Name("boris"), User.Password("password"))
      }
    }
  }
}
