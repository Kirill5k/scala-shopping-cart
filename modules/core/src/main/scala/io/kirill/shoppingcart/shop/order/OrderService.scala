package io.kirill.shoppingcart.shop.order

import cats.effect.Sync
import cats.implicits._
import io.kirill.shoppingcart.auth.user.UserId
import io.kirill.shoppingcart.common.errors.{OrderDoesNotBelongToThisUser, OrderNotFound}

trait OrderService[F[_]] {
  def get(userId: UserId, orderId: OrderId): F[Order]
  def findBy(userId: UserId): fs2.Stream[F, Order]
  def create(order: OrderCheckout): F[OrderId]
  def update(order: OrderPayment): F[Unit]
}

final private class LiveOrderService[F[_]: Sync](
    orderRepository: OrderRepository[F]
) extends OrderService[F] {

  override def get(userId: UserId, orderId: OrderId): F[Order] =
    orderRepository.find(orderId).flatMap {
      case None                          => OrderNotFound(orderId).raiseError[F, Order]
      case Some(o) if o.userId != userId => OrderDoesNotBelongToThisUser(o.id, userId).raiseError[F, Order]
      case Some(o)                       => o.pure[F]
    }

  override def findBy(userId: UserId): fs2.Stream[F, Order] =
    orderRepository.findBy(userId)

  override def create(order: OrderCheckout): F[OrderId] =
    orderRepository.create(order)

  override def update(order: OrderPayment): F[Unit] =
    orderRepository.update(order)
}

object OrderService {

  def make[F[_]: Sync](orderRepository: OrderRepository[F]): F[OrderService[F]] =
    Sync[F].delay(new LiveOrderService[F](orderRepository))
}
