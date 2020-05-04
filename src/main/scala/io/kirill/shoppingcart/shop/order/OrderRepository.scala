package io.kirill.shoppingcart.shop.order

import java.util.UUID

import cats.effect.{Resource, Sync}
import cats.implicits._
import io.circe.generic.auto._
import io.kirill.shoppingcart.auth.user.UserId
import io.kirill.shoppingcart.common.json._
import io.kirill.shoppingcart.common.persistence.Repository
import io.kirill.shoppingcart.shop.payment.PaymentId
import skunk._
import skunk.implicits._
import skunk.codec.all._
import skunk.circe.codec.all._
import squants.market.GBP

final class OrderRepository[F[_]: Sync] private (val sessionPool: Resource[F, Session[F]]) extends Repository[F, Order] {
  import OrderRepository._

  def findBy(userId: UserId): fs2.Stream[F, Order] =
    findManyBy(selectByUserId, userId.value)

  def find(id: OrderId): F[Option[Order]] =
    findOneBy(selectById, id.value)

  def create(order: OrderCheckout): F[OrderId] =
    run { session =>
      session.prepare(insert).use { cmd =>
        val orderId = OrderId(UUID.randomUUID())
        cmd.execute(orderId ~ order).map(_ => orderId)
      }
    }

  def update(order: OrderPayment): F[Unit] =
    runUpdateCommand(updatePayment, order)
}

object OrderRepository {
  private val decoder: Decoder[Order] =
    (uuid ~ varchar ~ uuid ~ uuid.opt ~ jsonb[Seq[OrderItem]] ~ numeric.map(GBP.apply)).map {
      case oid ~ status ~ uid ~ pid ~ items ~ total =>
        Order(OrderId(oid), OrderStatus(status), UserId(uid), pid.map(PaymentId), items, total)
    }

  private val encoder: Encoder[OrderId ~ OrderCheckout] =
    (uuid ~ varchar ~ uuid ~ uuid.opt ~ jsonb[Seq[OrderItem]] ~ numeric).contramap {
      case id ~ o =>
        id.value ~ o.status.value ~ o.userId.value ~ None ~ o.items ~ o.totalPrice.value
    }

  private val selectByUserId: Query[UUID, Order] =
    sql"""
         SELECT * FROM orders
         WHERE user_id = $uuid
         """.query(decoder)

  private val selectById: Query[UUID, Order] =
    sql"""
         SELECT * FROM orders
         WHERE id = $uuid
         """.query(decoder)

  private val insert: Command[OrderId ~ OrderCheckout] =
    sql"""
         INSERT INTO orders
         VALUES ($encoder)
         """.command

  private val updatePayment: Command[OrderPayment] =
    sql"""
         UPDATE orders
         SET payment_id = $uuid, status = $varchar
         WHERE id = $uuid
         """.command.contramap(o => o.paymentId.value ~ o.status.value ~ o.id.value)

  def make[F[_]: Sync](sessionPool: Resource[F, Session[F]]): F[OrderRepository[F]] =
    Sync[F].delay(new OrderRepository[F](sessionPool))
}
