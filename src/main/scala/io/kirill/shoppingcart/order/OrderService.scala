package io.kirill.shoppingcart.order

import io.kirill.shoppingcart.user.UserId

trait OrderService[F[_]] {
  def get(userId: UserId, orderId: OrderId): F[Option[Order]]
  def findBy(userId: UserId): F[Seq[Order]]
  def create(order: CreateOrder): F[OrderId]
}
