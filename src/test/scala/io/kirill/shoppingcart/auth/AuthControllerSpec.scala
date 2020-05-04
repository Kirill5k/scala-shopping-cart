package io.kirill.shoppingcart.auth

import java.util.UUID

import cats.effect.{ContextShift, IO}
import dev.profunktor.auth.jwt.JwtToken
import io.circe.generic.auto._
import io.circe.parser._
import io.kirill.shoppingcart.ControllerSpec
import io.kirill.shoppingcart.auth.AuthController._
import io.kirill.shoppingcart.common.json._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.literal._
import io.kirill.shoppingcart.auth.user.{Password, UserId, Username}
import io.kirill.shoppingcart.common.errors.InvalidUsernameOrPassword
import io.kirill.shoppingcart.common.web.RestController.ErrorResponse
import org.http4s.circe._
import org.http4s._
import org.http4s.implicits._

import scala.concurrent.ExecutionContext

class AuthControllerSpec extends ControllerSpec {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)

  val userId = UUID.fromString("d09c402a-8615-11ea-bc55-0242ac130003")

  "An AuthController" should {

    def loginRequestJson(name: String = "boris", password: String = "password"): Json =
      json"""{"username":$name,"password":$password}"""

    def createUserRequestJson(name: String = "boris", password: String = "password"): Json =
      json"""{"username":$name,"password":$password}"""

    "logout" should {
      "return no content on success" in {
        val authServiceMock = mock[AuthService[IO]]
        val controller      = new AuthController[IO](authServiceMock)

        when(authServiceMock.logout(any[JwtToken], any[Username])).thenReturn(IO.pure(()))

        val request                    = Request[IO](uri = uri"/auth/logout", method = Method.POST).withHeaders(Header("Authorization", "Bearer token"))
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[AuthLoginResponse](response, Status.NoContent, None)
        verify(authServiceMock).logout(JwtToken("token"), Username("Boris"))
      }
    }

    "login" should {
      "return token on success" in {
        val authServiceMock = mock[AuthService[IO]]
        val controller      = new AuthController[IO](authServiceMock)

        when(authServiceMock.login(any[Username], any[Password])).thenReturn(IO.pure(JwtToken("token")))

        val request                    = Request[IO](uri = uri"/auth/login", method = Method.POST).withEntity(loginRequestJson())
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[AuthLoginResponse](response, Status.Ok, Some(AuthLoginResponse("token")))
        verify(authServiceMock).login(Username("boris"), Password("password"))
      }

      "return forbidden when password is incorrect" in {
        val authServiceMock = mock[AuthService[IO]]
        val controller      = new AuthController[IO](authServiceMock)

        when(authServiceMock.login(any[Username], any[Password])).thenReturn(IO.raiseError(InvalidUsernameOrPassword))

        val request                    = Request[IO](uri = uri"/auth/login", method = Method.POST).withEntity(loginRequestJson())
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](response, Status.Forbidden, Some(ErrorResponse("Username or password is incorrect")))
        verify(authServiceMock).login(Username("boris"), Password("password"))
      }

      "return bad request when empty login or password" in {
        val authServiceMock = mock[AuthService[IO]]
        val controller      = new AuthController[IO](authServiceMock)

        val request                    = Request[IO](uri = uri"/auth/login", method = Method.POST).withEntity(loginRequestJson("", ""))
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](
          response,
          Status.BadRequest,
          Some(ErrorResponse("Predicate isEmpty() did not fail.: DownField(username)"))
        )
        verify(authServiceMock, never).login(any[Username], any[Password])
      }

      "return unprocessable entity when ivalid json" in {
        val authServiceMock = mock[AuthService[IO]]
        val controller      = new AuthController[IO](authServiceMock)

        val request                    = Request[IO](uri = uri"/auth/login", method = Method.POST).withEntity("foo")
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](response, Status.UnprocessableEntity, Some(ErrorResponse("""Could not decode JSON: "foo"""")))
        verify(authServiceMock, never).login(any[Username], any[Password])
      }

    }

    "create" should {
      "register new user on success" in {
        val authServiceMock = mock[AuthService[IO]]
        val controller      = new AuthController[IO](authServiceMock)

        when(authServiceMock.create(any[Username], any[Password])).thenReturn(IO.pure(JwtToken("token")))

        val request                    = Request[IO](uri = uri"/auth/create", method = Method.POST).withEntity(createUserRequestJson())
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[AuthLoginResponse](response, Status.Ok, Some(AuthLoginResponse("token")))
        verify(authServiceMock).create(Username("boris"), Password("password"))
      }
    }
  }
}
