package io.kirill.shoppingcart.shop.order

import java.util.UUID

import cats.effect.IO
import io.kirill.shoppingcart.auth.user.User
import io.kirill.shoppingcart.common.errors.{OrderDoesNotBelongToThisUser, OrderNotFound}
import io.kirill.shoppingcart.shop.payment.Payment
import org.mockito.scalatest.AsyncMockitoSugar
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers

class OrderServiceSpec extends AsyncFreeSpec with Matchers with AsyncMockitoSugar {

  val userId = User.Id(UUID.randomUUID())
  val order1 = OrderBuilder.order(userId = userId)

  "An OrderService" - {
    "get" - {
      "should find user's order" in {
        val result = for {
          repo <- repoMock
          _ = when(repo.find(order1.id)).thenReturn(IO.pure(Some(order1)))
          service <- OrderService.make(repo)
          order   <- service.get(userId, order1.id)
        } yield order

        result.unsafeToFuture().map(_ must be(order1))
      }

      "should return order not found error if not found" in {
        val result = for {
          repo <- repoMock
          _ = when(repo.find(order1.id)).thenReturn(IO.pure(None))
          service <- OrderService.make(repo)
          order   <- service.get(userId, order1.id)
        } yield order

        result.attempt.unsafeToFuture().map(_ mustBe Left(OrderNotFound(order1.id)))
      }

      "should return error if order does not belong to user" in {
        val result = for {
          repo <- repoMock
          _ = when(repo.find(order1.id)).thenReturn(IO.pure(Some(order1.copy(userId = User.Id(UUID.randomUUID())))))
          service <- OrderService.make(repo)
          order   <- service.get(userId, order1.id)
        } yield order

        result.attempt.unsafeToFuture().map(_ mustBe Left(OrderDoesNotBelongToThisUser(order1.id, userId)))
      }
    }

    "findBy" - {
      "should stream all user's orders" in {
        val result = for {
          repo <- repoMock
          _ = when(repo.findBy(userId)).thenReturn(fs2.Stream(order1).lift[IO])
          service <- OrderService.make(repo)
          orders  <- service.findBy(userId).compile.toList
        } yield orders

        result.unsafeToFuture().map(_ must be(List(order1)))
      }
    }

    "create" - {
      "should create new order" in {
        val result = for {
          repo <- repoMock
          checkout = OrderCheckout(userId, order1.items, order1.totalPrice, order1.status)
          _        = when(repo.create(checkout)).thenReturn(IO.pure(order1.id))
          service <- OrderService.make[IO](repo)
          id      <- service.create(checkout)
        } yield id

        result.unsafeToFuture().map(_ must be(order1.id))
      }
    }

    "update" - {
      "should update existing order" in {
        val result = for {
          repo <- repoMock
          paymentUpdate = OrderPayment(order1.id, Payment.Id(UUID.randomUUID()), order1.status)
          _             = when(repo.update(paymentUpdate)).thenReturn(IO.unit)
          service <- OrderService.make[IO](repo)
          res     <- service.update(paymentUpdate)
        } yield res

        result.unsafeToFuture().map(_ must be(()))
      }
    }
  }

  def repoMock: IO[OrderRepository[IO]] = IO(mock[OrderRepository[IO]])
}
