package io.kirill.shoppingcart.shop.order

import java.util.UUID

import cats.effect.IO
import io.kirill.shoppingcart.auth.user.UserId
import io.kirill.shoppingcart.common.errors.{AppError, OrderNotFound}
import io.kirill.shoppingcart.shop.cart.Quantity
import io.kirill.shoppingcart.shop.item.ItemId
import io.kirill.shoppingcart.shop.payment.PaymentId
import org.mockito.scalatest.AsyncMockitoSugar
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import squants.market.GBP

class OrderServiceSpec extends AsyncFreeSpec with Matchers with AsyncMockitoSugar {

  val userId = UserId(UUID.randomUUID())

  val order1 = Order(
    OrderId(UUID.randomUUID()),
    OrderStatus.awaitingPayment,
    userId,
    Some(PaymentId(UUID.randomUUID())),
    List(OrderItem(ItemId(UUID.randomUUID()), GBP(10), Quantity(2))),
    GBP(20)
  )

  "An OrderService" - {
    "get" - {
      "should find user's order" in {
        val result = for {
          repo <- repoMock
          _ = when(repo.find(order1.id)).thenReturn(IO.pure(Some(order1)))
          service <- OrderService.make(repo)
          order <- service.get(userId, order1.id)
        } yield order

        result.unsafeToFuture().map(_ must be (order1))
      }

      "should return order not found error if not found" in {
        val result = for {
          repo <- repoMock
          _ = when(repo.find(order1.id)).thenReturn(IO.pure(None))
          service <- OrderService.make(repo)
          order <- service.get(userId, order1.id)
        } yield order

        recoverToSucceededIf[OrderNotFound] {
          result.unsafeToFuture()
        }
      }

      "should return error if order does not belong to user" in {
        val result = for {
          repo <- repoMock
          _ = when(repo.find(order1.id)).thenReturn(IO.pure(Some(order1.copy(userId = UserId(UUID.randomUUID())))))
          service <- OrderService.make(repo)
          order <- service.get(userId, order1.id)
        } yield order

        recoverToSucceededIf[AppError] {
          result.unsafeToFuture()
        }
      }
    }

    "findBy" - {
      "should stream all user's orders" in {
        val result = for {
          repo <- repoMock
          _ = when(repo.findBy(userId)).thenReturn(fs2.Stream(order1).lift[IO])
          service <- OrderService.make(repo)
          orders <- service.findBy(userId).compile.toList
        } yield orders

        result.unsafeToFuture().map(_ must be (List(order1)))
      }
    }

    "create" - {
      "should create new order" in {
        val result = for {
          repo <- repoMock
          checkout = OrderCheckout(userId, order1.items, order1.totalPrice, order1.status)
          _ = when(repo.create(checkout)).thenReturn(IO.pure(order1.id))
          service <- OrderService.make[IO](repo)
          id      <- service.create(checkout)
        } yield id

        result.unsafeToFuture().map(_ must be (order1.id))
      }
    }

    "update" - {
      "should update existing order" in {
        val result = for {
          repo <- repoMock
          paymentUpdate = OrderPayment(order1.id, PaymentId(UUID.randomUUID()), order1.status)
          _ = when(repo.update(paymentUpdate)).thenReturn(IO.pure(order1.id))
          service <- OrderService.make[IO](repo)
          res      <- service.update(paymentUpdate)
        } yield res

        result.unsafeToFuture().map(_ must be (()))
      }
    }
  }

  def repoMock: IO[OrderRepository[IO]] = IO(mock[OrderRepository[IO]])
}
