package io.kirill.shoppingcart

import java.util.UUID

import cats.data.Kleisli
import cats.effect.IO
import eu.timepit.refined.string.Uuid
import io.kirill.shoppingcart.auth.{CommonUser, Password, User, UserId, Username}
import org.http4s.server.AuthMiddleware
import org.http4s.{EntityDecoder, Response, Status}
import org.mockito.ArgumentMatchersSugar
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

trait ControllerSpec extends AnyWordSpec with MockitoSugar with ArgumentMatchersSugar with Matchers {

  val authedUser = CommonUser(User(UserId(UUID.randomUUID()), Username("Boris"), Password("password")))

  val authMiddleware: AuthMiddleware[IO, CommonUser] = AuthMiddleware(Kleisli.pure(authedUser))

  def verifyResponse[A](actual: IO[Response[IO]], expectedStatus: Status, expectedBody: Option[A] = None)(
    implicit ev: EntityDecoder[IO, A]
  ): Unit = {
    val actualResp = actual.unsafeRunSync

    actualResp.status must be(expectedStatus)
    expectedBody match {
      case Some(expected) => actualResp.as[A].unsafeRunSync must be(expected)
      case None           => actualResp.body.compile.toVector.unsafeRunSync mustBe empty
    }
  }
}
