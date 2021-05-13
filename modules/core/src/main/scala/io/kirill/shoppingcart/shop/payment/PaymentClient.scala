package io.kirill.shoppingcart.shop.payment

import cats.Monad
import cats.effect.Sync
import cats.implicits._
import io.circe.generic.auto._
import io.kirill.shoppingcart.common.errors.PaymentError
import io.kirill.shoppingcart.common.web.JsonCodecs
import io.kirill.shoppingcart.config.PaymentConfig
import org.http4s.Method.POST
import org.http4s.{Status, Uri}
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl

trait PaymentClient[F[_]] {
  def process(payment: Payment): F[Payment.Id]
}

final private class LivePaymentClient[F[_]: Sync](
    config: PaymentConfig,
    client: Client[F]
) extends PaymentClient[F] with JsonCodecs with Http4sClientDsl[F] {

  override def process(payment: Payment): F[Payment.Id] =
    for {
      uri <- Uri.fromString(s"${config.baseUri}/payments").liftTo[F]
      req <- POST(payment, uri)
      id <- client.run(req).use { res =>
        if (res.status == Status.Ok || res.status == Status.Forbidden) res.as[Payment.Id]
        else PaymentError(res.status.reason).raiseError[F, Payment.Id]
      }
    } yield id
}

object PaymentClient {
  def make[F[_]: Sync](config: PaymentConfig, client: Client[F]): F[PaymentClient[F]] =
    Monad[F].pure(new LivePaymentClient[F](config, client))
}
