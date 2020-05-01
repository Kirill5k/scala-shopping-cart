package io.kirill.shoppingcart.shop.order

import java.util.UUID

import cats.effect.IO
import io.kirill.shoppingcart.PostgresRepositorySpec
import io.kirill.shoppingcart.auth.user.{EncryptedPassword, UserId, UserRepository, Username}
import io.kirill.shoppingcart.shop.cart.Quantity
import io.kirill.shoppingcart.shop.item.ItemId
import io.kirill.shoppingcart.shop.payment.PaymentId
import squants.market.GBP

class OrderRepositorySpec extends PostgresRepositorySpec {

  val orderItems: Seq[OrderItem] = List(
    OrderItem(ItemId(UUID.randomUUID()), GBP(BigDecimal(10)), Quantity(1)),
    OrderItem(ItemId(UUID.randomUUID()), GBP(BigDecimal(5.55)), Quantity(4)),
    OrderItem(ItemId(UUID.randomUUID()), GBP(BigDecimal(9.99)), Quantity(2))
  )

  "An OrderRepository" - {
    "find" - {
      "return order by id" in {
        val result = for {
          r <- OrderRepository.make(session)
          oid <- insertTestOrder(r)
          o <- r.find(oid)
        } yield o.get

        result.asserting { order =>
          order.items must be (orderItems)
          order.totalPrice must be (GBP(BigDecimal(25.54)))
        }
      }

      "return empty option if order does not exist" in {
        val orderRepository = OrderRepository.make(session)

        val result = orderRepository.flatMap(r => r.find(OrderId(UUID.randomUUID())))

        result.asserting(_ must be(None))
      }
    }

    "findBy" - {
      "return orders placed by user" in {
        val result = for {
          uid <- insertTestUser
          r <- OrderRepository.make(session)
          _ <- insertTestOrder(r)
          orders <- r.findBy(uid)
        } yield (orders, uid)

        result.asserting { case (orders, uid) =>
          orders must not be empty
          orders.map(_.userId) must contain only uid
        }
      }

      "return empty list if no matches" in {
        val orderRepository = OrderRepository.make(session)

        orderRepository.flatMap(_.findBy(UserId(UUID.randomUUID()))).asserting(_ must be(Nil))
      }
    }
  }

  def insertTestUser: IO[UserId] =
    for {
      r <- UserRepository.make(session)
      u <- r.findByName(Username("test-user"))
      uid <- u.fold(r.create(Username("test-user"), EncryptedPassword("password")))(x => IO.pure(x.id))
    } yield uid

  def insertTestOrder(repository: OrderRepository[IO]): IO[OrderId] =
    for {
      uid <- insertTestUser
      oid <- repository.create(CreateOrder(uid, PaymentId(UUID.randomUUID()), orderItems, GBP(BigDecimal(25.54))))
    } yield oid
}