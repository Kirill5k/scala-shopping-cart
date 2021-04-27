package io.kirill.shoppingcart

import java.util.UUID

import cats.data.Kleisli
import cats.effect.IO
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import io.kirill.shoppingcart.auth.{AdminUser, CommonUser}
import io.kirill.shoppingcart.auth.user._
import io.kirill.shoppingcart.common.json._
import org.http4s.server.AuthMiddleware
import org.http4s.{EntityDecoder, Response, Status}
import org.mockito.ArgumentMatchersSugar
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

trait ControllerSpec extends AnyWordSpec with MockitoSugar with ArgumentMatchersSugar with Matchers {
  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val authedUser = CommonUser(User(UserId(UUID.randomUUID()), Username("Boris"), Some(PasswordHash("password"))))
  val adminUser  = AdminUser(User(UserId(UUID.randomUUID()), Username("admin"), None))

  val authMiddleware: AuthMiddleware[IO, CommonUser] = AuthMiddleware(Kleisli.pure(authedUser))
  val adminMiddleware: AuthMiddleware[IO, AdminUser] = AuthMiddleware(Kleisli.pure(adminUser))

  def verifyResponse[A: Encoder](actual: IO[Response[IO]], expectedStatus: Status, expectedBody: Option[A] = None)(
      implicit ev: EntityDecoder[IO, A]
  ): Unit = {
    val actualResp = actual.unsafeRunSync

    actualResp.status must be(expectedStatus)
    expectedBody match {
      case Some(expected) => actualResp.as[Json].unsafeRunSync must be(expected.asJson)
      case None           => actualResp.body.compile.toVector.unsafeRunSync mustBe empty
    }
  }
}
