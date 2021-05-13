package io.kirill.shoppingcart.shop.payment

import cats.Monad
import cats.effect.Sync
import io.kirill.shoppingcart.config.PaymentConfig

trait PaymentClient[F[_]] {
  def process(payment: Payment): F[Payment.Id]
}

final private class LivePaymentClient[F[_]](
    config: PaymentConfig
) extends PaymentClient[F] {
  override def process(payment: Payment): F[Payment.Id] = ???
}

object PaymentClient {
  def make[F[_]: Sync](config: PaymentConfig): F[PaymentClient[F]] =
    Monad[F].pure(new LivePaymentClient[F](config))
}
