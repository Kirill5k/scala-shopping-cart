package io.kirill.shoppingcart.shop.cart

import java.util.UUID

import cats.effect.{Resource, Sync}
import cats.implicits._
import dev.profunktor.redis4cats.algebra.RedisCommands
import io.kirill.shoppingcart.auth.user.UserId
import io.kirill.shoppingcart.shop.item.ItemId

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
    redis: Resource[F, RedisCommands[F, String, Int]],
    exp: ShoppingCartExpiration
) extends CartService[F] {

  override def delete(userId: UserId): F[Unit] =
    redis.use(_.del(userId.value.toString))

  override def get(userId: UserId): F[Cart] =
    redis.use { r =>
      for {
        itemsMap <- r.hGetAll(userId.value.toString)
        cartItems = itemsMap.map { case (i, q) => CartItem(ItemId(UUID.fromString(i)), Quantity(q)) }
      } yield Cart(cartItems.toList)
    }

  override def removeItem(userId: UserId, itemId: ItemId): F[Unit] =
    redis.use(_.hDel(userId.value.toString, itemId.value.toString))

  override def add(userId: UserId, cart: Cart): F[Unit] =
    processItems(userId, cart.items) { case (r, ci) =>
        val uid = userId.value.toString
        val iid = ci.item.value.toString
        val q = ci.quantity
        r.hGet(uid, iid).flatMap(qOpt => r.hSet(uid, iid, qOpt.fold(q.value)(_+q.value)))
    }

  override def update(userId: UserId, cart: Cart): F[Unit] =
    processItems(userId, cart.items) { case (r, ci) =>
      val uid = userId.value.toString
      val iid = ci.item.value.toString
      val q = ci.quantity
        r.hExists(uid, iid).flatMap(e => if (e) r.hSet(userId.value.toString, iid, q.value) else Sync[F].pure(()))
    }

  private def processItems(userId: UserId, items: Seq[CartItem])(f: (RedisCommands[F, String, Int], CartItem) => F[Unit]): F[Unit] =
    redis.use { r =>
      items.map(i => f(r, i)).toList.sequence *> r.expire(userId.value.toString, exp.value)
    }
}

object CartService {

}
