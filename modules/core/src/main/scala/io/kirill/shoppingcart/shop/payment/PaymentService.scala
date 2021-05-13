package io.kirill.shoppingcart.shop.payment

import cats.Monad
import cats.effect.Sync

trait PaymentService[F[_]] {
  def process(payment: Payment): F[Payment.Id]
}

final private class LivePaymentService[F[_]](
    client: PaymentClient[F]
) extends PaymentService[F] {
  override def process(payment: Payment): F[Payment.Id] =
    client.process(payment)
}

object PaymentService {
  def make[F[_]: Sync](client: PaymentClient[F]): F[PaymentService[F]] =
    Monad[F].pure(new LivePaymentService[F](client))
}
