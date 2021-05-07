package io.kirill.shoppingcart.common

import cats.effect.Sync
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}
import squants.market.{GBP, Money}

object json extends JsonCodecs

trait JsonCodecs {

  implicit def coercibleDecoder[A: Coercible[B, *], B: Decoder]: Decoder[A] =
    Decoder[B].map(_.coerce[A])
  implicit def coercibleEncoder[A: Coercible[B, *], B: Encoder]: Encoder[A] =
    Encoder[B].contramap(_.asInstanceOf[B])

  implicit def deriveEntityEncoder[F[_], A: Encoder]: EntityEncoder[F, A]       = jsonEncoderOf[F, A]
  implicit def deriveEntityDecoder[F[_]: Sync, A: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]

  implicit val moneyEncoder: Encoder[Money] = Encoder[BigDecimal].contramap(_.amount)
  implicit val moneyDecoder: Decoder[Money] = Decoder[BigDecimal].map(GBP.apply)
}
