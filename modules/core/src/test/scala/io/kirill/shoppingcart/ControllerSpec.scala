package io.kirill.shoppingcart

import java.util.UUID
import cats.data.Kleisli
import cats.effect.{ContextShift, IO}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import io.kirill.shoppingcart.auth.{AdminUser, CommonUser}
import io.kirill.shoppingcart.auth.user._
import io.kirill.shoppingcart.common.web.json._
import org.http4s.server.AuthMiddleware
import org.http4s.{EntityDecoder, Response, Status}
import org.mockito.ArgumentMatchersSugar
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext

trait ControllerSpec extends AnyWordSpec with MockitoSugar with ArgumentMatchersSugar with Matchers {
  implicit val logger: Logger[IO]   = Slf4jLogger.getLogger[IO]
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  val authedUser = CommonUser(User(User.Id(UUID.randomUUID()), User.Name("Boris"), Some(User.PasswordHash("password"))))
  val adminUser  = AdminUser(User(User.Id(UUID.randomUUID()), User.Name("admin"), None))

  val authMiddleware: AuthMiddleware[IO, CommonUser] = AuthMiddleware(Kleisli.pure(authedUser))
  val adminMiddleware: AuthMiddleware[IO, AdminUser] = AuthMiddleware(Kleisli.pure(adminUser))

  def verifyResponse[A: Encoder](actual: IO[Response[IO]], expectedStatus: Status, expectedBody: Option[A] = None)(implicit
      ev: EntityDecoder[IO, A]
  ): Unit = {
    val actualResp = actual.unsafeRunSync

    actualResp.status mustBe expectedStatus
    expectedBody match {
      case Some(expected) => actualResp.as[Json].unsafeRunSync mustBe expected.asJson
      case None           => actualResp.body.compile.toVector.unsafeRunSync mustBe empty
    }
  }
}
