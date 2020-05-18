package io.kirill.shoppingcart.common

import cats.Applicative
import cats.effect.Sync
import dev.profunktor.auth.jwt.JwtToken
import io.circe.generic.extras.defaults._
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import io.circe.refined._
import io.kirill.shoppingcart.auth.user.{Password, PasswordHash, UserId, Username}
import io.kirill.shoppingcart.shop.brand.{BrandId, BrandName}
import io.kirill.shoppingcart.shop.cart.Quantity
import io.kirill.shoppingcart.shop.category.{CategoryId, CategoryName}
import io.kirill.shoppingcart.shop.item.{ItemDescription, ItemId, ItemName}
import io.kirill.shoppingcart.shop.order.{OrderId, OrderStatus}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}
import squants.market.{GBP, Money}

object json extends JsonCodecs {
  implicit def deriveEntityEncoder[F[_]: Applicative, A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
  implicit def deriveEntityDecoder[F[_]: Sync, A: Decoder]: EntityDecoder[F, A]        = jsonOf[F, A]
}

trait JsonCodecs {
  implicit val moneyEncoder: Encoder[Money] = Encoder[BigDecimal].contramap(_.amount)
  implicit val moneyDecoder: Decoder[Money] = Decoder[BigDecimal].map(GBP.apply)

  implicit val oidEncoder: Encoder[OrderId]         = deriveUnwrappedEncoder
  implicit val ostatusEncoder: Encoder[OrderStatus] = deriveUnwrappedEncoder

  implicit val bidEncoder: Encoder[BrandId] = deriveUnwrappedEncoder
  implicit val bidDecoder: Decoder[BrandId] = deriveUnwrappedDecoder

  implicit val cidEncoder: Encoder[CategoryId] = deriveUnwrappedEncoder
  implicit val cidDecoder: Decoder[CategoryId] = deriveUnwrappedDecoder

  implicit val iidEncoder: Encoder[ItemId] = deriveUnwrappedEncoder
  implicit val iidDecoder: Decoder[ItemId] = deriveUnwrappedDecoder

  implicit val inameEncoder: Encoder[ItemName]               = deriveUnwrappedEncoder
  implicit val idescriptionEncoder: Encoder[ItemDescription] = deriveUnwrappedEncoder
  implicit val bnamedEncoder: Encoder[BrandName]             = deriveUnwrappedEncoder
  implicit val cnameEncoder: Encoder[CategoryName]           = deriveUnwrappedEncoder

  implicit val quantityEncoder: Encoder[Quantity] = deriveUnwrappedEncoder
  implicit val quantityDecoder: Decoder[Quantity] = deriveUnwrappedDecoder

  implicit val uidDecoder: Decoder[UserId]                = deriveUnwrappedDecoder
  implicit val unameDecoder: Decoder[Username]            = deriveUnwrappedDecoder
  implicit val passwordHashDecoder: Decoder[PasswordHash] = deriveUnwrappedDecoder

  implicit val tokenEncoder: Encoder[JwtToken]            = deriveUnwrappedEncoder
  implicit val uidEncoder: Encoder[UserId]                = deriveUnwrappedEncoder
  implicit val unameEncoder: Encoder[Username]            = deriveUnwrappedEncoder
  implicit val passwordHashEncoder: Encoder[PasswordHash] = deriveUnwrappedEncoder
}
