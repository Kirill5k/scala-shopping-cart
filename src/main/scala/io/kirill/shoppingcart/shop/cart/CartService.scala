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
  def add(userId: UserId, cart: Cart): F[Unit]
  def update(userId: UserId, cart: Cart): F[Unit]
}

final class RedisCartService[F[_]: Sync] private (
    redis: RedisCommands[F, String, Int],
    exp: ShoppingCartExpiration
) extends CartService[F] {

  override def delete(userId: UserId): F[Unit] =
    redis.del(userId.value.toString)

  override def get(userId: UserId): F[Cart] =
    for {
      itemsMap <- redis.hGetAll(userId.value.toString)
      cartItems = itemsMap.map { case (i, q) => CartItem(ItemId(UUID.fromString(i)), Quantity(q)) }
    } yield Cart(cartItems.toList)

  override def removeItem(userId: UserId, itemId: ItemId): F[Unit] =
    redis.hDel(userId.value.toString, itemId.value.toString)

  override def add(userId: UserId, cart: Cart): F[Unit] =
    processItems(userId, cart.items) { ci =>
        val uid = userId.value.toString
        val iid = ci.item.value.toString
        val q = ci.quantity
        redis.hGet(uid, iid).flatMap(qOpt => redis.hSet(uid, iid, qOpt.fold(q.value)(_+q.value)))
    }

  override def update(userId: UserId, cart: Cart): F[Unit] =
    processItems(userId, cart.items) { ci =>
      val uid = userId.value.toString
      val iid = ci.item.value.toString
      val q = ci.quantity
        redis.hExists(uid, iid).flatMap(e => if (e) redis.hSet(userId.value.toString, iid, q.value) else Sync[F].pure(()))
    }

  private def processItems(userId: UserId, items: Seq[CartItem])(f: CartItem => F[Unit]): F[Unit] =
    items.map(f).toList.sequence *> redis.expire(userId.value.toString, exp.value)
}
