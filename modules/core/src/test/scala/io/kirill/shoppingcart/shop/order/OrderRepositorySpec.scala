package io.kirill.shoppingcart.shop.order

import java.util.UUID

import cats.effect.IO
import io.kirill.shoppingcart.PostgresRepositorySpec
import io.kirill.shoppingcart.auth.user.{User, UserRepository}
import io.kirill.shoppingcart.shop.item.Item
import io.kirill.shoppingcart.shop.payment.Payment
import squants.market.GBP

class OrderRepositorySpec extends PostgresRepositorySpec {

  val orderItems: Seq[OrderItem] = List(
    OrderItem(Item.Id(UUID.randomUUID()), GBP(BigDecimal(10)), Item.Quantity(1)),
    OrderItem(Item.Id(UUID.randomUUID()), GBP(BigDecimal(5.55)), Item.Quantity(4)),
    OrderItem(Item.Id(UUID.randomUUID()), GBP(BigDecimal(9.99)), Item.Quantity(2))
  )

  "An OrderRepository" - {
    "find" - {
      "return order by id" in {
        val result = for {
          r   <- OrderRepository.make(session)
          oid <- insertTestOrder(r)
          o   <- r.find(oid)
        } yield o.get

        result.asserting { order =>
          order.items must be(orderItems)
          order.totalPrice must be(GBP(BigDecimal(25.54)))
          order.status must be(Order.Status.AwaitingPayment)
          order.paymentId must be(None)
        }
      }

      "return empty option if order does not exist" in {
        val orderRepository = OrderRepository.make(session)

        val result = orderRepository.flatMap(r => r.find(Order.Id(UUID.randomUUID())))

        result.asserting(_ must be(None))
      }
    }

    "update" - {
      "update payment id on the existing order" in {
        val paymentId = Payment.Id(UUID.randomUUID())
        val result = for {
          r   <- OrderRepository.make(session)
          oid <- insertTestOrder(r)
          _   <- r.update(OrderPayment(oid, paymentId))
          o   <- r.find(oid)
        } yield o.get

        result.asserting { order =>
          order.status must be(Order.Status.Processing)
          order.paymentId must be(Some(paymentId))
        }
      }
    }

    "findBy" - {
      "return orders placed by user" in {
        val result = for {
          uid    <- insertTestUser
          r      <- OrderRepository.make(session)
          _      <- insertTestOrder(r)
          orders <- r.findBy(uid).compile.toList
        } yield (orders, uid)

        result.asserting {
          case (orders, uid) =>
            orders must not be empty
            orders.map(_.userId) must contain only uid
        }
      }

      "return empty list if no matches" in {
        val orderRepository = OrderRepository.make(session)

        orderRepository.flatMap(_.findBy(User.Id(UUID.randomUUID())).compile.toList).asserting(_ must be(Nil))
      }
    }
  }

  def insertTestUser: IO[User.Id] =
    for {
      r   <- UserRepository.make(session)
      u   <- r.findByName(User.Name("test-user"))
      uid <- u.fold(r.create(User.Name("test-user"), User.PasswordHash("password")))(x => IO.pure(x.id))
    } yield uid

  def insertTestOrder(repository: OrderRepository[IO]): IO[Order.Id] =
    for {
      uid <- insertTestUser
      oid <- repository.create(OrderCheckout(uid, orderItems.toList, GBP(BigDecimal(25.54))))
    } yield oid
}
