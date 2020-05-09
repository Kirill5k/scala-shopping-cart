package io.kirill.shoppingcart.shop.payment

trait PaymentService[F[_]] {
  def process(payment: Payment): F[PaymentId]
}
