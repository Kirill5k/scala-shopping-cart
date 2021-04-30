package io.kirill.shoppingcart.shop.order

import java.util.UUID

import cats.effect.{IO}
import io.circe._
import io.circe.generic.auto._
import io.circe.literal._
import io.kirill.shoppingcart.ControllerSpec
import io.kirill.shoppingcart.auth.user.User
import io.kirill.shoppingcart.common.errors.{ItemNotFound, OrderDoesNotBelongToThisUser, OrderNotFound}
import io.kirill.shoppingcart.common.json._
import io.kirill.shoppingcart.common.web.ErrorResponse
import io.kirill.shoppingcart.shop.cart.{Cart, CartItem, CartService}
import io.kirill.shoppingcart.shop.item.{Item, ItemBuilder, ItemService}
import io.kirill.shoppingcart.shop.order.OrderController.{OrderCheckoutResponse, OrderResponse}
import io.kirill.shoppingcart.shop.payment.{Card, Payment, PaymentService}
import eu.timepit.refined.auto._
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import squants.market.GBP

class OrderControllerSpec extends ControllerSpec {

  val paymentId = Payment.Id(UUID.randomUUID())

  val item1Id = Item.Id(UUID.fromString("607995e0-8e3a-11ea-bc55-0242ac130003"))
  val item1   = ItemBuilder.item("item-1", GBP(14.99), id = item1Id)
  val item2   = ItemBuilder.item("item-2", GBP(5.99))

  val order1Id = Order.Id(UUID.fromString("666665e0-8e3a-11ea-bc55-0242ac130003"))
  val order1   = OrderBuilder.order(id = order1Id)

  "An OrderController" should {

    "/orders" should {
      "return all orders that belong to the current user" in {
        val (os, cs, is, ps) = mocks
        val controller       = new OrderController[IO](os, cs, is, ps)

        when(os.findBy(any[User.Id])).thenReturn(fs2.Stream(order1).lift[IO])

        val request  = Request[IO](uri = uri"/orders", method = Method.GET)
        val response = controller.routes(authMiddleware).orNotFound.run(request)

        val expectedResponse = List(
          OrderResponse(
            order1.id,
            order1.status,
            order1.items,
            order1.totalPrice
          )
        )
        verifyResponse[List[OrderResponse]](response, Status.Ok, Some(expectedResponse))
        verify(os).findBy(authedUser.value.id)
      }
    }

    "/orders/{id}" should {
      "find order by id that belong to the current user" in {
        val (os, cs, is, ps) = mocks
        val controller       = new OrderController[IO](os, cs, is, ps)

        when(os.get(any[User.Id], any[Order.Id])).thenReturn(IO.pure(order1))

        val request  = Request[IO](uri = uri"/orders/666665e0-8e3a-11ea-bc55-0242ac130003", method = Method.GET)
        val response = controller.routes(authMiddleware).orNotFound.run(request)

        val expectedResponse =
          OrderResponse(
            order1.id,
            order1.status,
            order1.items,
            order1.totalPrice
          )
        verifyResponse[OrderResponse](response, Status.Ok, Some(expectedResponse))
        verify(os).get(authedUser.value.id, order1Id)
      }

      "return 404 when order not found" in {
        val (os, cs, is, ps) = mocks
        val controller       = new OrderController[IO](os, cs, is, ps)

        when(os.get(any[User.Id], any[Order.Id])).thenReturn(IO.raiseError(OrderNotFound(order1Id)))

        val request  = Request[IO](uri = uri"/orders/666665e0-8e3a-11ea-bc55-0242ac130003", method = Method.GET)
        val response = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](
          response,
          Status.NotFound,
          Some(ErrorResponse("Order with id 666665e0-8e3a-11ea-bc55-0242ac130003 does not exist"))
        )
        verify(os).get(authedUser.value.id, order1Id)
      }

      "return 403 when order does not belong to user" in {
        val (os, cs, is, ps) = mocks
        val controller       = new OrderController[IO](os, cs, is, ps)

        when(os.get(any[User.Id], any[Order.Id]))
          .thenReturn(IO.raiseError(OrderDoesNotBelongToThisUser(order1Id, authedUser.value.id)))

        val request  = Request[IO](uri = uri"/orders/666665e0-8e3a-11ea-bc55-0242ac130003", method = Method.GET)
        val response = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](response, Status.Forbidden, Some(ErrorResponse("Order does not belong to this user")))
        verify(os).get(authedUser.value.id, order1Id)
      }
    }

    "/orders/checkout" should {
      "place an order" in {
        val (os, cs, is, ps) = mocks
        val controller       = new OrderController[IO](os, cs, is, ps)

        val cart    = Cart(List(CartItem(item1.id, Item.Quantity(2)), CartItem(item2.id, Item.Quantity(1))))
        val orderId = Order.Id(UUID.randomUUID())
        when(cs.get(any[User.Id])).thenReturn(IO.pure(cart))
        when(is.findById(any[Item.Id])).thenReturn(IO.pure(item1)).andThen(IO.pure(item2))
        when(os.create(any[OrderCheckout])).thenReturn(IO.pure(orderId))
        when(cs.delete(any[User.Id])).thenReturn(IO.unit)

        val request  = Request[IO](uri = uri"/orders/checkout", method = Method.POST)
        val response = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[OrderCheckoutResponse](response, Status.Created, Some(OrderCheckoutResponse(orderId)))
        verify(cs).get(authedUser.value.id)
        verify(is).findById(item1Id)
        verify(is).findById(item2.id)
        verify(os).create(
          OrderCheckout(
            authedUser.value.id,
            List(OrderItem(item1.id, item1.price, Item.Quantity(2)), OrderItem(item2.id, item2.price, Item.Quantity(1))),
            GBP(35.97)
          )
        )
        verify(cs).delete(authedUser.value.id)
      }

      "return 404 if one of the items does not exist" in {
        val (os, cs, is, ps) = mocks
        val controller       = new OrderController[IO](os, cs, is, ps)

        val cart = Cart(List(CartItem(item1.id, Item.Quantity(1)), CartItem(item2.id, Item.Quantity(2))))
        when(cs.get(any[User.Id])).thenReturn(IO.pure(cart))
        when(is.findById(any[Item.Id])).thenReturn(IO.raiseError(ItemNotFound(item1.id)))

        val request  = Request[IO](uri = uri"/orders/checkout", method = Method.POST)
        val response = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](
          response,
          Status.NotFound,
          Some(ErrorResponse("Item with id 607995e0-8e3a-11ea-bc55-0242ac130003 does not exist"))
        )
        verify(cs).get(authedUser.value.id)
        verify(is).findById(item1Id)
        verify(is).findById(item2.id)
      }

      "return empty cart error if cart is empty" in {
        val (os, cs, is, ps) = mocks
        val controller       = new OrderController[IO](os, cs, is, ps)

        when(cs.get(any[User.Id])).thenReturn(IO.pure(Cart(Nil)))

        val request  = Request[IO](uri = uri"/orders/checkout", method = Method.POST)
        val response = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](response, Status.BadRequest, Some(ErrorResponse("Unable to checkout empty cart")))
        verify(cs).get(authedUser.value.id)
      }
    }

    "/orders/{id}/payment" should {

      "process payment and update order" in {
        val (os, cs, is, ps) = mocks
        val controller       = new OrderController[IO](os, cs, is, ps)

        when(os.get(any[User.Id], any[Order.Id])).thenReturn(IO.pure(order1))
        when(ps.process(any[Payment])).thenReturn(IO.pure(paymentId))
        when(os.update(any[OrderPayment])).thenReturn(IO.unit)

        val request = Request[IO](uri = uri"/orders/666665e0-8e3a-11ea-bc55-0242ac130003/payment", method = Method.POST)
          .withEntity(paymentReqJson())
        val response = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](response, Status.NoContent)
        verify(os).get(authedUser.value.id, order1Id)
        verify(ps).process(Payment(order1, Card("Boris", 1234123412341234L, "1221", 123)))
        verify(os).update(OrderPayment(order1Id, paymentId))
      }

      "return 404 when order not found" in {
        val (os, cs, is, ps) = mocks
        val controller       = new OrderController[IO](os, cs, is, ps)

        when(os.get(any[User.Id], any[Order.Id])).thenReturn(IO.raiseError(OrderNotFound(order1Id)))

        val request = Request[IO](uri = uri"/orders/666665e0-8e3a-11ea-bc55-0242ac130003/payment", method = Method.POST)
          .withEntity(paymentReqJson())
        val response = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](
          response,
          Status.NotFound,
          Some(ErrorResponse("Order with id 666665e0-8e3a-11ea-bc55-0242ac130003 does not exist"))
        )
        verify(os).get(authedUser.value.id, order1Id)
      }

      "return 403 when order does not belong to user" in {
        val (os, cs, is, ps) = mocks
        val controller       = new OrderController[IO](os, cs, is, ps)

        when(os.get(any[User.Id], any[Order.Id]))
          .thenReturn(IO.raiseError(OrderDoesNotBelongToThisUser(order1Id, authedUser.value.id)))

        val request = Request[IO](uri = uri"/orders/666665e0-8e3a-11ea-bc55-0242ac130003/payment", method = Method.POST)
          .withEntity(paymentReqJson())
        val response = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](response, Status.Forbidden, Some(ErrorResponse("Order does not belong to this user")))
        verify(os).get(authedUser.value.id, order1Id)
      }

      "return bad request when name has unexpected format" in {
        val (os, cs, is, ps) = mocks
        val controller       = new OrderController[IO](os, cs, is, ps)

        val request = Request[IO](uri = uri"/orders/d09c402a-8615-11ea-bc55-0242ac130003/payment", method = Method.POST)
          .withEntity(paymentReqJson(name = "123"))
        val response = controller.routes(authMiddleware).orNotFound.run(request)

        val expectedResponse = ErrorResponse(
          """Predicate failed: "123".matches("^[a-zA-Z]+(([',. -][a-zA-Z ])?[a-zA-Z]*)*$").: DownField(name),DownField(card)"""
        )
        verifyResponse[ErrorResponse](response, Status.BadRequest, Some(expectedResponse))
      }

      "return bad request when expiration has unexpected format" in {
        val (os, cs, is, ps) = mocks
        val controller       = new OrderController[IO](os, cs, is, ps)

        val request = Request[IO](uri = uri"/orders/d09c402a-8615-11ea-bc55-0242ac130003/payment", method = Method.POST)
          .withEntity(paymentReqJson(expiration = "5a"))
        val response = controller.routes(authMiddleware).orNotFound.run(request)

        val expectedResponse = ErrorResponse("""Predicate failed: "5a".matches("^[0-9]{4}$").: DownField(expiration),DownField(card)""")
        verifyResponse[ErrorResponse](response, Status.BadRequest, Some(expectedResponse))
      }

      "return bad request when cvv has unexpected format" in {
        val (os, cs, is, ps) = mocks
        val controller       = new OrderController[IO](os, cs, is, ps)

        val request = Request[IO](uri = uri"/orders/d09c402a-8615-11ea-bc55-0242ac130003/payment", method = Method.POST)
          .withEntity(paymentReqJson(cvv = 9999))
        val response = controller.routes(authMiddleware).orNotFound.run(request)

        val expectedResponse = ErrorResponse(
          """Right predicate of ((9999 > 0) && !(9999 > 999)) failed: Predicate (9999 > 999) did not fail.: DownField(cvv),DownField(card)"""
        )
        verifyResponse[ErrorResponse](response, Status.BadRequest, Some(expectedResponse))
      }
    }
  }

  def paymentReqJson(
      name: String = "Boris",
      number: Long = 1234123412341234L,
      expiration: String = "1221",
      cvv: Int = 123
  ): Json =
    json"""
      {"card": {
          "name": $name,
          "number": $number,
          "expiration": $expiration,
          "cvv": $cvv
        }
      }
      """

  def mocks: (OrderService[IO], CartService[IO], ItemService[IO], PaymentService[IO]) =
    (mock[OrderService[IO]], mock[CartService[IO]], mock[ItemService[IO]], mock[PaymentService[IO]])
}
