package io.kirill.shoppingcart.shop.item

import java.util.UUID

import cats.data.Validated.{Invalid, Valid}
import cats.effect.Sync
import io.circe._
import io.circe.generic.auto._
import io.circe.refined._
import cats.implicits._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import org.typelevel.log4cats.Logger
import io.kirill.shoppingcart.auth.AdminUser
import io.kirill.shoppingcart.common.web.RestController
import io.kirill.shoppingcart.common.json._
import io.kirill.shoppingcart.shop.brand.{BrandId, BrandName}
import io.kirill.shoppingcart.shop.category.{CategoryId, CategoryName}
import org.http4s.{AuthedRoutes, HttpRoutes, ParseFailure, QueryParamDecoder}
import org.http4s.dsl.impl.OptionalValidatingQueryParamDecoderMatcher
import org.http4s.server.{AuthMiddleware, Router}
import squants.Money

final class ItemController[F[_]: Sync: Logger](itemService: ItemService[F]) extends RestController[F] {
  import RestController._
  import ItemController._

  private val prefixPath = "/items"

  private val publicHttpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(itemId) =>
      withErrorHandling {
        Ok(itemService.findById(ItemId(itemId)).map(ItemResponse.from))
      }
    case GET -> Root :? ItemQueryParams(brand) =>
      withErrorHandling {
        brand match {
          case Some(Invalid(errors)) => BadRequest(errors.map(_.details).mkString_(","))
          case Some(Valid(brand))    => Ok(itemService.findBy(brand.toDomain).map(ItemResponse.from).compile.toList)
          case None                  => Ok(itemService.findAll.map(ItemResponse.from).compile.toList)
        }
      }
  }

  private val adminHttpRoutes: AuthedRoutes[AdminUser, F] = AuthedRoutes.of {
    case adminReq @ PUT -> Root / UUIDVar(itemId) as _ =>
      withErrorHandling {
        for {
          update <- adminReq.req.decodeR[ItemUpdateRequest]
          _      <- itemService.update(UpdateItem(ItemId(itemId), update.price))
          res    <- NoContent()
        } yield res
      }
    case adminReq @ POST -> Root as _ =>
      withErrorHandling {
        for {
          r <- adminReq.req.decodeR[ItemCreateRequest]
          item = CreateItem(ItemName(r.name.value), ItemDescription(r.description.value), r.price, r.brandId, r.categoryId)
          id  <- itemService.create(item)
          res <- Created(ItemCreateResponse(id))
        } yield res
      }
  }

  def routes(adminAuthMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] =
    Router(
      prefixPath            -> publicHttpRoutes,
      "/admin" + prefixPath -> adminAuthMiddleware(adminHttpRoutes)
    )
}

object ItemController {
  final case class BrandParam(value: String) extends AnyVal {
    def toDomain: BrandName = BrandName(value.toLowerCase.capitalize)
  }

  implicit val brandParamDecoder: QueryParamDecoder[BrandParam] = QueryParamDecoder[String]
    .emap(b => Either.cond(!b.isBlank, b, ParseFailure(b, "Brand must not be blank")))
    .map(BrandParam.apply)

  object ItemQueryParams extends OptionalValidatingQueryParamDecoderMatcher[BrandParam]("brand")

  final case class ItemResponse(
      id: ItemId,
      name: ItemName,
      description: ItemDescription,
      price: Money,
      brand: BrandName,
      category: CategoryName
  )

  object ItemResponse {
    def from(item: Item): ItemResponse =
      ItemResponse(
        item.id,
        item.name,
        item.description,
        item.price,
        item.brand.name,
        item.category.name
      )
  }

  final case class ItemUpdateRequest(price: Money)

  type NonEmptyString = String Refined NonEmpty

  final case class ItemCreateRequest(
      name: NonEmptyString,
      description: NonEmptyString,
      price: Money,
      brandId: BrandId,
      categoryId: CategoryId
  )

  final case class ItemCreateResponse(itemId: ItemId)

  def make[F[_]: Sync: Logger](is: ItemService[F]): F[ItemController[F]] =
    Sync[F].delay(new ItemController[F](is))
}
