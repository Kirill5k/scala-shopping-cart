package io.kirill.shoppingcart.shop.category

import cats.effect.Sync
import cats.implicits._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.auto._
import io.circe.refined._
import io.kirill.shoppingcart.auth.AdminUser
import io.kirill.shoppingcart.common.web.RestController
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.typelevel.log4cats.Logger

final class CategoryController[F[_]: Sync: Logger](categoryService: CategoryService[F]) extends RestController[F] {
  import CategoryController._
  private val prefixPath = "/categories"

  private val publicHttpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    withErrorHandling {
      Ok(categoryService.findAll)
    }
  }

  private val adminHttpRoutes: AuthedRoutes[AdminUser, F] = AuthedRoutes.of { case adminReq @ POST -> Root as _ =>
    withErrorHandling {
      for {
        req <- adminReq.req.as[CategoryCreateRequest]
        id  <- categoryService.create(Category.Name(req.name.value.capitalize))
        res <- Created(CategoryCreateResponse(id))
      } yield res
    }
  }

  def routes(adminAuthMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] =
    Router(
      prefixPath            -> publicHttpRoutes,
      "/admin" + prefixPath -> adminAuthMiddleware(adminHttpRoutes)
    )
}

object CategoryController {
  final case class CategoryCreateRequest(name: NonEmptyString)
  final case class CategoryCreateResponse(id: Category.Id)

  def make[F[_]: Sync: Logger](cs: CategoryService[F]): F[CategoryController[F]] =
    Sync[F].pure(new CategoryController[F](cs))
}
