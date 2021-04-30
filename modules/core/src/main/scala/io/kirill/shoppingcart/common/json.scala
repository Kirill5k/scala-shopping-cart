package io.kirill.shoppingcart.common

import cats.effect.Sync
import dev.profunktor.auth.jwt.JwtToken
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import io.circe.refined._
import io.estatico.newtype.Coercible
import io.kirill.shoppingcart.auth.user.User
import io.kirill.shoppingcart.shop.brand.Brand
import io.kirill.shoppingcart.shop.category.Category
import io.kirill.shoppingcart.shop.item.Item
import io.kirill.shoppingcart.shop.order.Order
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}
import squants.market.{GBP, Money}

object json extends JsonCodecs

trait JsonCodecs {

  implicit def coercibleDecoder[A: Coercible[B, *], B: Decoder]: Decoder[A] =
    Decoder[B].map(b => Coercible[B, A].apply(b))
  implicit def coercibleEncoder[A: Coercible[B, *], B: Encoder]: Encoder[A] =
    Encoder[B].contramap(_.asInstanceOf[B])

  implicit def deriveEntityEncoder[F[_], A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
  implicit def deriveEntityDecoder[F[_]: Sync, A: Decoder]: EntityDecoder[F, A]        = jsonOf[F, A]

  implicit val moneyEncoder: Encoder[Money] = Encoder[BigDecimal].contramap(_.amount)
  implicit val moneyDecoder: Decoder[Money] = Decoder[BigDecimal].map(GBP.apply)

  implicit val oidEncoder: Encoder[Order.Id]         = deriveUnwrappedEncoder
  implicit val ostatusEncoder: Encoder[Order.Status] = deriveUnwrappedEncoder

  implicit val bidEncoder: Encoder[Brand.Id] = deriveConfiguredEncoder
  implicit val bidDecoder: Decoder[Brand.Id] = deriveConfiguredDecoder

  implicit val cidEncoder: Encoder[Category.Id] = deriveConfiguredEncoder
  implicit val cidDecoder: Decoder[Category.Id] = deriveConfiguredDecoder

  implicit val iidEncoder: Encoder[Item.Id] = deriveConfiguredEncoder
  implicit val iidDecoder: Decoder[Item.Id] = deriveConfiguredDecoder

  implicit val inameEncoder: Encoder[Item.Name]               = deriveConfiguredEncoder
  implicit val idescriptionEncoder: Encoder[Item.Description] = deriveConfiguredEncoder
  implicit val bnamedEncoder: Encoder[Brand.Name]             = deriveConfiguredEncoder
  implicit val cnameEncoder: Encoder[Category.Name]           = deriveConfiguredEncoder

  implicit val quantityEncoder: Encoder[Item.Quantity] = deriveConfiguredEncoder
  implicit val quantityDecoder: Decoder[Item.Quantity] = deriveConfiguredDecoder

  implicit val uidDecoder: Decoder[User.Id]                = deriveConfiguredDecoder
  implicit val unameDecoder: Decoder[User.Name]            = deriveConfiguredDecoder
  implicit val passwordHashDecoder: Decoder[User.PasswordHash] = deriveConfiguredDecoder

  implicit val tokenEncoder: Encoder[JwtToken]            = deriveConfiguredEncoder
  implicit val uidEncoder: Encoder[User.Id]                = deriveConfiguredEncoder
  implicit val unameEncoder: Encoder[User.Name]            = deriveConfiguredEncoder
  implicit val passwordHashEncoder: Encoder[User.PasswordHash] = deriveConfiguredEncoder
}
