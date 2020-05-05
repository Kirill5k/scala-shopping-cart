package io.kirill.shoppingcart.shop.order

import io.kirill.shoppingcart.auth.user.UserId

trait OrderService[F[_]] {
  // throw order not found or order does not belong to user
  def get(userId: UserId, orderId: OrderId): F[Order]
  def findBy(userId: UserId): F[Seq[Order]]
  def create(order: OrderCheckout): F[OrderId]
  def update(order: OrderPayment): F[Unit]
}
