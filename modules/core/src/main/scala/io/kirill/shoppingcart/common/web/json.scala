package io.kirill.shoppingcart.common.web

import cats.effect.Sync
import cats.implicits._
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder, ParseFailure, QueryParamDecoder}
import squants.market.{GBP, Money}

object json extends JsonCodecs

trait JsonCodecs {

  implicit def coercibleQueryParamDecoder[A: Coercible[B, *], B: QueryParamDecoder]: QueryParamDecoder[A] =
    QueryParamDecoder[B].map(_.coerce[A])

  implicit def refinedParamDecoder[T: QueryParamDecoder, P](implicit
      ev: Validate[T, P]
  ): QueryParamDecoder[T Refined P] =
    QueryParamDecoder[T].emap(refineV[P](_).leftMap(m => ParseFailure(m, m)))

  implicit def coercibleDecoder[A: Coercible[B, *], B: Decoder]: Decoder[A] =
    Decoder[B].map(_.coerce[A])
  implicit def coercibleEncoder[A: Coercible[B, *], B: Encoder]: Encoder[A] =
    Encoder[B].contramap(_.asInstanceOf[B])

  implicit def deriveEntityEncoder[F[_], A: Encoder]: EntityEncoder[F, A]       = jsonEncoderOf[F, A]
  implicit def deriveEntityDecoder[F[_]: Sync, A: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]

  implicit val moneyEncoder: Encoder[Money] = Encoder[BigDecimal].contramap(_.amount)
  implicit val moneyDecoder: Decoder[Money] = Decoder[BigDecimal].map(GBP.apply)

//  implicit def decodeUnwrapped[A](implicit decode: UnwrappedDecoder[A]): Decoder[A] = decode
//  implicit def encodeUnwrapped[A](implicit encode: UnwrappedEncoder[A]): Encoder[A] = encode
}
