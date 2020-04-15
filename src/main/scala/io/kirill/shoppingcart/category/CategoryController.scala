package io.kirill.shoppingcart.category

import io.circe.generic.auto._
import cats.{Defer, Monad}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

final class CategoryController[F[_]: Defer: Monad](categoryService: CategoryService[F]) extends Http4sDsl[F] {
  private val prefixPath = "/categories"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => Ok(categoryService.findAll)
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
