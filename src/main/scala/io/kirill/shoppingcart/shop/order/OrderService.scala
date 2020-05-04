package io.kirill.shoppingcart.shop.order

import io.kirill.shoppingcart.auth.user.UserId

trait OrderService[F[_]] {
  def get(userId: UserId, orderId: OrderId): F[Option[Order]]
  def findBy(userId: UserId): F[Seq[Order]]
  def create(order: OrderCheckout): F[OrderId]
  def update(order: OrderPayment): F[Unit]
}
