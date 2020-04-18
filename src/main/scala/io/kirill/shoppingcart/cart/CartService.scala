package io.kirill.shoppingcart.cart

import cats.implicits._
import io.kirill.shoppingcart.auth.UserId
import io.kirill.shoppingcart.item.ItemId

trait CartService[F[_]] {
  def delete(userId: UserId): F[Unit]
  def get(userId: UserId): F[Cart]
  def removeItem(userId: UserId, itemId: ItemId): F[Unit]
  def update(userId: UserId, items: Map[ItemId, Quantity]): F[Unit]
}
