package io.kirill.shoppingcart.item

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import cats.{Defer, Monad}
import io.kirill.shoppingcart.brand.BrandName
import org.http4s.circe._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.{EntityEncoder, HttpRoutes, QueryParamDecoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import org.http4s.server.Router
import squants.market.Money

final class ItemController[F[_]: Defer: Monad](itemService: ItemService[F]) extends Http4sDsl[F] {
  import ItemController._

  private val prefixPath = "/items"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? ItemQueryParams(brand) =>
      Ok(brand.fold(itemService.findAll)(b => itemService.findBy(b.toDomain)))
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}

object ItemController {
  implicit val moneyEncoder: Encoder[Money] =
    Encoder[BigDecimal].contramap(_.amount)

  final case class BrandParam(value: String) extends AnyVal {
    def toDomain: BrandName = BrandName(value.toLowerCase.capitalize)
  }

  implicit val brandParamDecoder: QueryParamDecoder[BrandParam] = QueryParamDecoder[String].map(BrandParam.apply)

  object ItemQueryParams extends OptionalQueryParamDecoderMatcher[BrandParam]("brand")
}
