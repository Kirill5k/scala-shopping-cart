package io.kirill.shoppingcart.payment

trait PaymentService[F[_]] {
  def process(payment: Payment): F[PaymentId]
}
