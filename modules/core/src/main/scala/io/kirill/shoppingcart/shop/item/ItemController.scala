package io.kirill.shoppingcart.shop.item

import java.util.UUID

import cats.data.Validated.{Invalid, Valid}
import cats.effect.Sync
import io.circe._
import io.circe.generic.auto._
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import io.kirill.shoppingcart.common.web.RestController
import io.kirill.shoppingcart.common.json._
import io.kirill.shoppingcart.shop.brand.BrandName
import io.kirill.shoppingcart.shop.category.CategoryName
import org.http4s.{HttpRoutes, ParseFailure, QueryParamDecoder}
import org.http4s.dsl.impl.OptionalValidatingQueryParamDecoderMatcher
import org.http4s.server.Router
import squants.Money

final class ItemController[F[_]: Sync: Logger](itemService: ItemService[F]) extends RestController[F] {
  import ItemController._

  private val prefixPath = "/items"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
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

  val routes: HttpRoutes[F] =
    Router(prefixPath -> httpRoutes)
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

  def make[F[_]: Sync: Logger](is: ItemService[F]): F[ItemController[F]] =
    Sync[F].delay(new ItemController[F](is))
}
