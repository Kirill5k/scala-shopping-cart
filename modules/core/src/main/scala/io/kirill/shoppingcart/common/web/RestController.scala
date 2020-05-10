package io.kirill.shoppingcart.common.web

import cats.effect.Sync
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import io.circe._
import io.circe.generic.auto._
import io.kirill.shoppingcart.common.errors._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{Challenge, InvalidMessageBodyFailure, Request, Response}

trait RestController[F[_]] extends Http4sDsl[F] {
  import RestController._
  import io.kirill.shoppingcart.common.json._

  protected def withErrorHandling(
                                   response: => F[Response[F]]
                                 )(
    implicit s: Sync[F],
    l: Logger[F]
  ): F[Response[F]] =
    response.handleErrorWith {
      case e @ OrderDoesNotBelongToThisUser(oid, uid) =>
        l.error(s"user ${uid.value} attempted to view order ${oid.value}") *>
          Forbidden(ErrorResponse(e.getMessage))
      case e @ InvalidUsernameOrPassword(username) =>
        l.error(s"failed attempt to login by user ${username.value}") *>
          Forbidden(ErrorResponse(e.getMessage))
      case e @ AuthTokenNotPresent(username) =>
        l.error(s"failed attempt to access secure content without auth token by ${username.value}") *>
          Unauthorized(Challenge(scheme = "Bearer", realm = e.getMessage))
      case e: BadRequestError =>
        l.error(s"bad request error: ${e.getMessage}") *>
          BadRequest(ErrorResponse(e.getMessage))
      case e: NotFoundError =>
        l.error(s"entity not found error: ${e.getMessage}")
        NotFound(ErrorResponse(e.getMessage))
      case InvalidMessageBodyFailure(details, cause) =>
        cause match {
          case Some(c) => l.error(s"error parsing json: ${c.getMessage}\n ${details}") *> BadRequest(ErrorResponse(c.getMessage))
          case _ => l.error(s"malformed json: $details") *> UnprocessableEntity(ErrorResponse(details))
        }
      case error =>
        l.error(error)(s"unexpected error: ${error.getMessage}") *>
          InternalServerError(ErrorResponse(error.getMessage))
    }
}

object RestController {

  final case class ErrorResponse(message: String)

  implicit class RequestDecoder[F[_]: Sync](private val req: Request[F]) {
    def decodeR[A: Decoder]: F[A] = {
      req.asJsonDecode[A]
    }
  }
}
