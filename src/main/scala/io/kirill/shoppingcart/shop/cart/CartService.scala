package io.kirill.shoppingcart.shop.cart

import java.util.UUID

import cats.effect.{Resource, Sync}
import cats.implicits._
import dev.profunktor.redis4cats.algebra.RedisCommands
import io.kirill.shoppingcart.auth.user.UserId
import io.kirill.shoppingcart.config.AppConfig
import io.kirill.shoppingcart.shop.item.ItemId

trait CartService[F[_]] {
  def delete(userId: UserId): F[Unit]
  def get(userId: UserId): F[Cart]
  def removeItem(userId: UserId, itemId: ItemId): F[Unit]
  def add(userId: UserId, cart: Cart): F[Unit]
  def update(userId: UserId, cart: Cart): F[Unit]
}

final private class RedisCartService[F[_]: Sync](
    redis: Resource[F, RedisCommands[F, String, String]]
)(
    implicit config: AppConfig
) extends CartService[F] {

  override def delete(userId: UserId): F[Unit] =
    redis.use(_.del(userId.value.toString))

  override def get(userId: UserId): F[Cart] =
    redis.use { r =>
      for {
        itemsMap <- r.hGetAll(userId.value.toString)
        cartItems = itemsMap.map { case (i, q) => CartItem(ItemId(UUID.fromString(i)), Quantity(q.toInt)) }
      } yield Cart(cartItems.toList)
    }

  override def removeItem(userId: UserId, itemId: ItemId): F[Unit] =
    redis.use(_.hDel(userId.value.toString, itemId.value.toString))

  override def add(userId: UserId, cart: Cart): F[Unit] =
    processItems(userId, cart.items) {
      case (r, ci) =>
        val uid = userId.value.toString
        val iid = ci.item.value.toString
        val q   = ci.quantity.value
        r.hGet(uid, iid).flatMap(qOpt => r.hSet(uid, iid, qOpt.fold(q.toString)(x => (x.toInt + q).toString)))
    }

  override def update(userId: UserId, cart: Cart): F[Unit] =
    processItems(userId, cart.items) {
      case (r, ci) =>
        val uid = userId.value.toString
        val iid = ci.item.value.toString
        val q   = ci.quantity.value.toString
        r.hExists(uid, iid).flatMap(e => if (e) r.hSet(userId.value.toString, iid, q) else ().pure[F])
    }

  private def processItems(userId: UserId, items: Seq[CartItem])(f: (RedisCommands[F, String, String], CartItem) => F[Unit]): F[Unit] =
    redis.use { r =>
      items.map(i => f(r, i)).toList.sequence *> r.expire(userId.value.toString, config.shop.cartExpiration)
    }
}

object CartService {

  def redisCartService[F[_]: Sync](
      redis: Resource[F, RedisCommands[F, String, String]]
  )(
      implicit config: AppConfig
  ): F[CartService[F]] = Sync[F].delay(new RedisCartService[F](redis))
}
