package io.kirill.shoppingcart.shop.order

import io.kirill.shoppingcart.auth.UserId

trait OrderService[F[_]] {
  def get(orderId: OrderId): F[Option[Order]]
  def findBy(userId: UserId): F[Seq[Order]]
  def create(order: CreateOrder): F[OrderId]
}
