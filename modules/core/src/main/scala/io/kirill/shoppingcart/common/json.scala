package io.kirill.shoppingcart.common

import cats.effect.Sync
import dev.profunktor.auth.jwt.JwtToken
import io.circe.generic.extras.decoding.UnwrappedDecoder
import io.circe.generic.extras.encoding.UnwrappedEncoder
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

  implicit def decodeUnwrapped[A](implicit decode: UnwrappedDecoder[A]): Decoder[A] = decode
  implicit def encodeUnwrapped[A](implicit encode: UnwrappedEncoder[A]): Encoder[A] = encode
}
