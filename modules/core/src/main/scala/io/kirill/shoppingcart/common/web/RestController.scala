package io.kirill.shoppingcart.common.web

import cats.effect.Sync
import cats.implicits._
import io.circe.generic.auto._
import io.kirill.shoppingcart.common.errors._
import org.http4s.dsl.Http4sDsl
import org.http4s.{Challenge, InvalidMessageBodyFailure, ParseFailure, Response}
import org.typelevel.log4cats.Logger

final case class ErrorResponse(message: String)

trait RestController[F[_]] extends Http4sDsl[F] with JsonCodecs {

  protected def withErrorHandling(
      response: => F[Response[F]]
  )(implicit
      F: Sync[F],
      logger: Logger[F]
  ): F[Response[F]] =
    response.handleErrorWith {
      case e @ OrderDoesNotBelongToThisUser(oid, uid) =>
        logger.error(s"user ${uid.value} attempted to view order ${oid.value}") *>
          Forbidden(ErrorResponse(e.getMessage))
      case e @ InvalidUsernameOrPassword(username) =>
        logger.error(s"failed attempt to login by user ${username.value}") *>
          Forbidden(ErrorResponse(e.getMessage))
      case e @ AuthTokenNotPresent(username) =>
        logger.error(s"failed attempt to access secure content without auth token by ${username.value}") *>
          Unauthorized(Challenge(scheme = "Bearer", realm = e.getMessage))
      case e: BadRequestError =>
        logger.error(s"bad request error: ${e.getMessage}") *>
          BadRequest(ErrorResponse(e.getMessage))
      case e: NotFoundError =>
        logger.error(s"entity not found error: ${e.getMessage}")
        NotFound(ErrorResponse(e.getMessage))
      case ParseFailure(_, message) =>
        logger.error(s"error processing a request: $message") *>
          BadRequest(ErrorResponse(message))
      case InvalidMessageBodyFailure(details, cause) =>
        cause match {
          case Some(c) => logger.error(s"error parsing json: ${c.getMessage}\n$details") *> BadRequest(ErrorResponse(c.getMessage))
          case _       => logger.error(s"malformed json: $details") *> UnprocessableEntity(ErrorResponse(details))
        }
      case error =>
        logger.error(error)(s"unexpected error: ${error.getMessage}") *>
          InternalServerError(ErrorResponse(error.getMessage))
    }
}
