package io.kirill.shoppingcart.shop.brand

import cats.effect.Sync
import cats.implicits._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import org.typelevel.log4cats.Logger
import io.circe.generic.auto._
import io.circe.refined._
import io.kirill.shoppingcart.auth.AdminUser
import io.kirill.shoppingcart.common.web.RestController
import io.kirill.shoppingcart.common.json._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.server.{AuthMiddleware, Router}

final class BrandController[F[_]: Sync: Logger](brandService: BrandService[F]) extends RestController[F] {
  import RestController._
  import BrandController._

  private val prefixPath = "/brands"

  private val publicHttpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root =>
      withErrorHandling {
        Ok(brandService.findAll.compile.toList)
      }
  }

  private val adminHttpRoutes: AuthedRoutes[AdminUser, F] = AuthedRoutes.of {
    case adminReq @ POST -> Root as _ =>
      withErrorHandling {
        for {
          req <- adminReq.req.decodeR[BrandCreateRequest]
          id  <- brandService.create(BrandName(req.name.value.capitalize))
          res <- Created(BrandCreateResponse(id))
        } yield res
      }
  }

  def routes(adminAuthMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] = Router(
    prefixPath            -> publicHttpRoutes,
    "/admin" + prefixPath -> adminAuthMiddleware(adminHttpRoutes)
  )
}

object BrandController {
  type NonEmptyString = String Refined NonEmpty

  final case class BrandCreateRequest(name: NonEmptyString)
  final case class BrandCreateResponse(brandId: BrandId)

  def make[F[_]: Sync: Logger](bs: BrandService[F]): F[BrandController[F]] =
    Sync[F].delay(new BrandController[F](bs))
}
