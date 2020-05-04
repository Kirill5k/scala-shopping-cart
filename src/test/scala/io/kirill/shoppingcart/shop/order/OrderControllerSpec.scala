package io.kirill.shoppingcart.shop.order

import cats.effect.{ContextShift, IO}
import io.circe._
import io.circe.generic.auto._
import io.kirill.shoppingcart.ControllerSpec
import io.kirill.shoppingcart.auth.user.UserId
import io.kirill.shoppingcart.common.json._
import io.kirill.shoppingcart.common.web.RestController.ErrorResponse
import io.kirill.shoppingcart.shop.cart.{Cart, CartService}
import io.kirill.shoppingcart.shop.item.ItemService
import io.kirill.shoppingcart.shop.payment.PaymentService
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._

import scala.concurrent.ExecutionContext

class OrderControllerSpec extends ControllerSpec {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)

  "An OrderController" should {

    "checkout" should {
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
