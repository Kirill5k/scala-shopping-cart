package io.kirill.shoppingcart.shop.cart

import java.util.UUID

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.redis4cats.algebra.RedisCommands
import io.kirill.shoppingcart.auth.UserId
import io.kirill.shoppingcart.shop.item.{ItemId, ItemService}
import squants.market.GBP

import scala.concurrent.duration.FiniteDuration

final case class ShoppingCartExpiration(value: FiniteDuration) extends AnyVal

trait CartService[F[_]] {
  def delete(userId: UserId): F[Unit]
  def get(userId: UserId): F[Cart]
  def removeItem(userId: UserId, itemId: ItemId): F[Unit]
  def add(userId: UserId, items: Map[ItemId, Quantity]): F[Unit]
  def update(userId: UserId, items: Map[ItemId, Quantity]): F[Unit]
}

final class RedisCartService[F[_]: Sync] private (
    itemService: ItemService[F],
    redis: RedisCommands[F, String, Int],
    exp: ShoppingCartExpiration
) extends CartService[F] {

  override def delete(userId: UserId): F[Unit] =
    redis.del(userId.value.toString)

  override def get(userId: UserId): F[Cart] =
    for {
      itemsMap <- redis.hGetAll(userId.value.toString)
      itemIds = itemsMap.map { case (i, q) => (ItemId(UUID.fromString(i)), Quantity(q)) }
      cartItems <- itemIds.map { case (i, q) => itemService.findById(i).map(CartItem(_, q)) }.toList.sequence
      total = GBP(cartItems.map(ci => ci.item.price.value * ci.quantity.value).sum)
    } yield Cart(cartItems, total)

  override def removeItem(userId: UserId, itemId: ItemId): F[Unit] =
    redis.hDel(userId.value.toString, itemId.value.toString)

  override def add(userId: UserId, items: Map[ItemId, Quantity]): F[Unit] =
    processItems(userId, items) {
      case (i, q) =>
        val uid = userId.value.toString
        val iid = i.value.toString
        redis.hGet(uid, iid).flatMap(qOpt => redis.hSet(uid, iid, qOpt.fold(q.value)(_+q.value)))
    }

  override def update(userId: UserId, items: Map[ItemId, Quantity]): F[Unit] =
    processItems(userId, items) {
      case (i, q) =>
        val uid = userId.value.toString
        val iid = i.value.toString
        redis.hExists(uid, iid).flatMap(e => if (e) redis.hSet(userId.value.toString, i.value.toString, q.value) else Sync[F].pure(()))
    }

  private def processItems(userId: UserId, items: Map[ItemId, Quantity])(f: ((ItemId, Quantity)) => F[Unit]): F[Unit] =
    items.map(f).toList.sequence *> redis.expire(userId.value.toString, exp.value)
}
