package io.kirill.shoppingcart.common.web

import cats.effect.Sync
import cats.implicits._
import io.circe.generic.auto._
import io.kirill.shoppingcart.common.web.RestController.ErrorResponse
import io.kirill.shoppingcart.common.errors._
import org.http4s.{ParseFailure, Response, Status}
import org.http4s.dsl.Http4sDsl

trait RestController[F[_]] extends Http4sDsl[F] {
  import json._

  protected def withErrorHandling(response: => F[Response[F]])(implicit s: Sync[F]): F[Response[F]] =
    response.handleErrorWith {
      case error: ParseFailure =>
        BadRequest(ErrorResponse(error.details))
      case ItemNotFound(message) =>
        NotFound(ErrorResponse(message))
      case ProcessingError(message) =>
        BadRequest(ErrorResponse(message))
      case error =>
        InternalServerError(ErrorResponse(error.getMessage()))
    }
}

object RestController {
  final case class ErrorResponse(message: String)
}
