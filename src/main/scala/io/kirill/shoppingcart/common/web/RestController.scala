package io.kirill.shoppingcart.common.web

import cats.effect.Sync
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.refined._
import io.kirill.shoppingcart.common.errors._
import org.http4s.circe._
import org.http4s.{Challenge, InvalidMessageBodyFailure, ParseFailure, Request, Response, Status}
import org.http4s.dsl.Http4sDsl

trait RestController[F[_]] extends Http4sDsl[F] {
  import RestController._
  import json._

  protected def withErrorHandling(response: => F[Response[F]])(implicit s: Sync[F]): F[Response[F]] =
    response.handleErrorWith {
      case e @ AuthTokenNotPresent =>
        Unauthorized(Challenge(scheme = "Bearer", realm = e.getMessage))
      case e @ InvalidUsernameOrPassword =>
        Forbidden(ErrorResponse(e.getMessage))
      case error: ParseFailure =>
        BadRequest(ErrorResponse(error.details))
      case ItemNotFound(message) =>
        NotFound(ErrorResponse(message))
      case ProcessingError(message) =>
        BadRequest(ErrorResponse(message))
      case InvalidMessageBodyFailure(details, cause) =>
        cause match {
          case Some(c) if c.getMessage.startsWith("Predicate") => BadRequest(ErrorResponse(c.getMessage))
          case _ => UnprocessableEntity(ErrorResponse(details))
        }
      case error =>
        InternalServerError(ErrorResponse(error.getMessage))
    }
}

object RestController {
  import io.kirill.shoppingcart.common.web.json._

  final case class ErrorResponse(message: String)

  implicit class RequestDecoder[F[_]: Sync](private val req: Request[F]) {
    def decodeR[A: Decoder]: F[A] = {
      req.asJsonDecode[A]
    }
  }
}
