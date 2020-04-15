package io.kirill.shoppingcart.item

import cats.data.Validated.{Invalid, Valid}
import io.circe._
import io.circe.generic.auto._
import cats.{Defer, Monad}
import cats.implicits._
import io.kirill.shoppingcart.brand.BrandName
import org.http4s.circe.CirceEntityCodec._
import org.http4s.{HttpRoutes, ParseFailure, QueryParamDecoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.{OptionalValidatingQueryParamDecoderMatcher}
import org.http4s.server.Router
import squants.market.Money

final class ItemController[F[_]: Defer: Monad](itemService: ItemService[F]) extends Http4sDsl[F] {
  import ItemController._

  private val prefixPath = "/items"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? ItemQueryParams(brand) =>
      brand match {
        case Some(Invalid(errors)) => BadRequest(errors.map(_.details).mkString_(","))
        case Some(Valid(brand)) => Ok(itemService.findBy(brand.toDomain))
        case None => Ok(itemService.findAll)
      }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}

object ItemController {
  implicit val moneyEncoder: Encoder[Money] = Encoder[BigDecimal].contramap(_.amount)

  final case class BrandParam(value: String) extends AnyVal {
    def toDomain: BrandName = BrandName(value.toLowerCase.capitalize)
  }

  implicit val brandParamDecoder: QueryParamDecoder[BrandParam] = QueryParamDecoder[String]
    .emap(b => Either.cond(!b.isBlank, b, ParseFailure(b, "Brand must not be blank")))
    .map(BrandParam.apply)

  object ItemQueryParams extends OptionalValidatingQueryParamDecoderMatcher[BrandParam]("brand")
}
