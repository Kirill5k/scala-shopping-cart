package io.kirill.shoppingcart.order

import io.kirill.shoppingcart.common.security.user.UserId

trait OrderService[F[_]] {
  def get(orderId: OrderId): F[Option[Order]]
  def findBy(userId: UserId): F[Seq[Order]]
  def create(order: CreateOrder): F[OrderId]
}
