package io.kirill.shoppingcart.shop.category

import cats.effect.Sync
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import io.circe.generic.auto._
import io.kirill.shoppingcart.common.web.RestController
import io.kirill.shoppingcart.common.json._
import org.http4s.HttpRoutes
import org.http4s.server.Router

final class CategoryController[F[_]: Sync: Logger](categoryService: CategoryService[F]) extends RestController[F] {
  private val prefixPath = "/categories"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root =>
      withErrorHandling {
        Ok(categoryService.findAll)
      }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}

object CategoryController {
  def make[F[_]: Sync: Logger](cs: CategoryService[F]): F[CategoryController[F]] =
    Sync[F].delay(new CategoryController[F](cs))
}
