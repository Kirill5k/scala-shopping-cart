package io.kirill.shoppingcart.shop.order

import java.util.UUID

import cats.effect.{Resource, Sync}
import cats.implicits._
import io.circe.generic.auto._
import io.kirill.shoppingcart.auth.UserId
import io.kirill.shoppingcart.common.json._
import io.kirill.shoppingcart.common.persistence.Repository
import io.kirill.shoppingcart.shop.payment.PaymentId
import skunk._
import skunk.implicits._
import skunk.codec.all._
import skunk.circe.codec.all._
import squants.market.GBP


final class OrderRepository[F[_]: Sync] private(val sessionPool: Resource[F, Session[F]]) extends Repository[F] {
  import OrderRepository._

  def findBy(userId: UserId): F[List[Order]] =
    run { session =>
      session.prepare(selectByUserId).use { ps =>
        ps.stream(userId.value, 1024).compile.toList
      }
    }

  def find(id: OrderId): F[Option[Order]] =
    run { session =>
      session.prepare(selectById).use { ps =>
        ps.option(id.value)
      }
    }

  def create(order: CreateOrder): F[OrderId] =
    run { session =>
      session.prepare(insert).use { cmd =>
        val orderId = OrderId(UUID.randomUUID())
        cmd.execute(orderId ~ order).map(_ => orderId)
      }
    }
}

object OrderRepository {
  private val decoder: Decoder[Order] =
    (uuid ~ uuid ~ uuid ~ jsonb[Seq[OrderItem]] ~ numeric.map(GBP.apply)).map {
      case oid ~ uid ~ pid ~ items ~ total =>
        Order(OrderId(oid), UserId(uid), PaymentId(pid), items, total)
    }

  private val encoder: Encoder[OrderId ~ CreateOrder] =
    (uuid ~ uuid ~ uuid ~ jsonb[Seq[OrderItem]] ~ numeric).contramap { case id ~ o =>
      id.value ~ o.userId.value ~ o.paymentId.value ~ o.items ~ o.totalPrice.value
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

  private val insert: Command[OrderId ~ CreateOrder] =
    sql"""
         INSERT INTO orders
         VALUES ($encoder)
         """.command

  def make[F[_]: Sync](sessionPool: Resource[F, Session[F]]): F[OrderRepository[F]] =
    Sync[F].delay(new OrderRepository[F](sessionPool))
}



