package io.kirill.shoppingcart.shop.order

import java.util.UUID

import cats.effect.{ContextShift, IO}
import io.circe._
import io.circe.generic.auto._
import io.kirill.shoppingcart.ControllerSpec
import io.kirill.shoppingcart.auth.user.UserId
import io.kirill.shoppingcart.common.errors.ItemNotFound
import io.kirill.shoppingcart.common.json._
import io.kirill.shoppingcart.common.web.RestController.ErrorResponse
import io.kirill.shoppingcart.shop.cart.{Cart, CartItem, CartService, Quantity}
import io.kirill.shoppingcart.shop.item.{ItemBuilder, ItemId, ItemService}
import io.kirill.shoppingcart.shop.order.OrderController.OrderCheckoutResponse
import io.kirill.shoppingcart.shop.payment.PaymentService
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import squants.market.GBP

import scala.concurrent.ExecutionContext

class OrderControllerSpec extends ControllerSpec {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)

  val item1Id = ItemId(UUID.fromString("607995e0-8e3a-11ea-bc55-0242ac130003"))
  val item1 = ItemBuilder.item("item-1", GBP(14.99)).copy(id = item1Id)
  val item2 = ItemBuilder.item("item-2", GBP(5.99))

  "An OrderController" should {

    "checkout" should {
      "place an order" in {
        val (os, cs, is, ps) = mocks
        val controller = new OrderController[IO](os, cs, is, ps)

        val cart = Cart(List(CartItem(item1.id, Quantity(2)), CartItem(item2.id, Quantity(1))))
        val orderId = OrderId(UUID.randomUUID())
        when(cs.get(any[UserId])).thenReturn(IO.pure(cart))
        when(is.findById(any[ItemId])).thenReturn(IO.pure(item1)).andThen(IO.pure(item2))
        when(os.create(any[OrderCheckout])).thenReturn(IO.pure(orderId))
        when(cs.delete(any[UserId])).thenReturn(IO.pure(()))

        val request = Request[IO](uri = uri"/orders/checkout", method = Method.POST)
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[OrderCheckoutResponse](response, Status.Created, Some(OrderCheckoutResponse(orderId.value)))
        verify(cs).get(authedUser.value.id)
        verify(is).findById(item1Id)
        verify(is).findById(item2.id)
        verify(os).create(OrderCheckout(authedUser.value.id, List(OrderItem(item1.id, item1.price, Quantity(2)), OrderItem(item2.id, item2.price, Quantity(1))), GBP(35.97)))
        verify(cs).delete(authedUser.value.id)
      }

      "return 404 if one of the items does not exist" in {
        val (os, cs, is, ps) = mocks
        val controller = new OrderController[IO](os, cs, is, ps)

        val cart = Cart(List(CartItem(item1.id, Quantity(1)), CartItem(item2.id, Quantity(2))))
        when(cs.get(any[UserId])).thenReturn(IO.pure(cart))
        when(is.findById(any[ItemId])).thenReturn(IO.raiseError(ItemNotFound(item1.id)))

        val request = Request[IO](uri = uri"/orders/checkout", method = Method.POST)
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](response, Status.NotFound, Some(ErrorResponse("Item with id 607995e0-8e3a-11ea-bc55-0242ac130003 does not exist")))
        verify(cs).get(authedUser.value.id)
        verify(is).findById(item1Id)
        verify(is).findById(item2.id)
      }

      "return empty cart error if cart is empty" in {
        val (os, cs, is, ps) = mocks
        val controller = new OrderController[IO](os, cs, is, ps)

        when(cs.get(any[UserId])).thenReturn(IO.pure(Cart(Nil)))

        val request = Request[IO](uri = uri"/orders/checkout", method = Method.POST)
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](response, Status.BadRequest, Some(ErrorResponse("Unable to checkout empty cart")))
        verify(cs).get(authedUser.value.id)
      }
    }
  }

  def mocks: (OrderService[IO], CartService[IO], ItemService[IO], PaymentService[IO]) =
    (mock[OrderService[IO]], mock[CartService[IO]], mock[ItemService[IO]], mock[PaymentService[IO]])
}
