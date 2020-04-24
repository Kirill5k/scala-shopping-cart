package io.kirill.shoppingcart.auth

import cats.effect.{ContextShift, IO}
import io.circe.generic.auto._
import io.circe.parser._
import io.kirill.shoppingcart.ControllerSpec
import io.kirill.shoppingcart.auth.AuthController.{AuthLoginRequest, AuthLoginResponse}
import io.kirill.shoppingcart.common.web.json._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.literal._
import io.kirill.shoppingcart.common.web.RestController.ErrorResponse
import org.http4s.circe._
import org.http4s._
import org.http4s.implicits._

import scala.concurrent.ExecutionContext

class AuthControllerSpec extends ControllerSpec {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)

  "An AuthController" should {

    def loginRequestJson(name: String = "boris", password: String = "password"): Json =
      json"""{"username":$name,"password":$password}"""

    "login" in {
      val authServiceMock = mock[AuthService[IO]]
      val controller      = new AuthController[IO](authServiceMock)

      when(authServiceMock.login(any[Username], any[Password])).thenReturn(IO.pure(JwtToken("token")))

      val request                    = Request[IO](uri = uri"/auth/login", method = Method.POST).withEntity(loginRequestJson())
      val response: IO[Response[IO]] = controller.routes.orNotFound.run(request)

      verifyResponse[AuthLoginResponse](response, Status.Ok, Some(AuthLoginResponse("token")))
      verify(authServiceMock).login(Username("boris"), Password("password"))
    }

    "return bad request when empty login or password" in {
      val authServiceMock = mock[AuthService[IO]]
      val controller      = new AuthController[IO](authServiceMock)

      val request                    = Request[IO](uri = uri"/auth/login", method = Method.POST).withEntity(loginRequestJson("", ""))
      val response: IO[Response[IO]] = controller.routes.orNotFound.run(request)

      verifyResponse[ErrorResponse](response, Status.BadRequest, Some(ErrorResponse("Predicate isEmpty() did not fail.: DownField(username)")))
      verify(authServiceMock, never).login(any[Username], any[Password])
    }

    "return unprocessable entity when ivalid json" in {
      val authServiceMock = mock[AuthService[IO]]
      val controller      = new AuthController[IO](authServiceMock)

      val request                    = Request[IO](uri = uri"/auth/login", method = Method.POST).withEntity("foo")
      val response: IO[Response[IO]] = controller.routes.orNotFound.run(request)

      verifyResponse[ErrorResponse](response, Status.UnprocessableEntity, Some(ErrorResponse("""Could not decode JSON: "foo"""")))
      verify(authServiceMock, never).login(any[Username], any[Password])
    }
  }
}
