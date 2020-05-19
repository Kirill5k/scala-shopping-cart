package io.kirill.shoppingcart.shop.category

import cats.effect.Sync
import cats.implicits._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import io.chrisdavenport.log4cats.Logger
import io.circe.generic.auto._
import io.circe.refined._
import io.kirill.shoppingcart.auth.AdminUser
import io.kirill.shoppingcart.common.web.RestController
import io.kirill.shoppingcart.common.json._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.server.{AuthMiddleware, Router}

final class CategoryController[F[_]: Sync: Logger](categoryService: CategoryService[F]) extends RestController[F] {
  import RestController._
  import CategoryController._
  private val prefixPath = "/categories"

  private val publicHttpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root =>
      withErrorHandling {
        Ok(categoryService.findAll.compile.toList)
      }
  }

  private val adminHttpRoutes: AuthedRoutes[AdminUser, F] = AuthedRoutes.of {
    case adminReq @ POST -> Root as _ =>
      withErrorHandling {
        for {
          req <- adminReq.req.decodeR[CategoryCreateRequest]
          id  <- categoryService.create(CategoryName(req.name.value.capitalize))
          res <- Created(CategoryCreateResponse(id))
        } yield res
      }
  }

  def routes(adminAuthMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] = Router(
    prefixPath            -> publicHttpRoutes,
    "/admin" + prefixPath -> adminAuthMiddleware(adminHttpRoutes)
  )
}

object CategoryController {
  type NonEmptyString = String Refined NonEmpty

  final case class CategoryCreateRequest(name: NonEmptyString)
  final case class CategoryCreateResponse(brandId: CategoryId)

  def make[F[_]: Sync: Logger](cs: CategoryService[F]): F[CategoryController[F]] =
    Sync[F].delay(new CategoryController[F](cs))
}
