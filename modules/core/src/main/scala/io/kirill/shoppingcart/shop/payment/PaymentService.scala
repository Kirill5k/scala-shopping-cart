package io.kirill.shoppingcart.shop.payment

import cats.effect.Sync

trait PaymentService[F[_]] {
  def process(payment: Payment): F[PaymentId]
}

object PaymentService {
  def make[F[_]: Sync](): F[PaymentService[F]] = ???
}
