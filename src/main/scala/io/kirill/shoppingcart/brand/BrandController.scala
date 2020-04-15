package io.kirill.shoppingcart.brand

import io.circe.generic.auto._
import cats.{Defer, Monad}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

final class BrandController[F[_]: Defer: Monad](brandService: BrandService[F]) extends Http4sDsl[F] {
  private val prefixPath = "/brands"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => Ok(brandService.findAll)
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
