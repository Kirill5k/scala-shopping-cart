package io.kirill.shoppingcart.shop.cart

import java.util.UUID

import cats.effect._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.literal._
import io.kirill.shoppingcart.ControllerSpec
import io.kirill.shoppingcart.auth.user.User
import io.kirill.shoppingcart.common.json._
import io.kirill.shoppingcart.shop.item.Item
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import org.http4s.{Method, Request, Response}

class CartControllerSpec extends ControllerSpec {

  val item1Id = Item.Id(UUID.fromString("607995e0-8e3a-11ea-bc55-0242ac130003"))

  val cart = Cart(List(CartItem(item1Id, Item.Quantity(4))))

  "A CartController" should {

    "GET /shopping-cat" should {
      "return shopping cart of a current user" in {
        val cartService = mockService
        val controller  = new CartController[IO](cartService)

        when(cartService.get(any[User.Id])).thenReturn(IO.pure(cart))

        val request                    = Request[IO](uri = uri"/shopping-cart", method = Method.GET)
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[Cart](response, Status.Ok, Some(cart))
        verify(cartService).get(authedUser.value.id)
      }
    }

    "DELETE /shopping-cat/{itemId}" should {
      "delete shopping cart of a current user" in {
        val cartService = mockService
        val controller  = new CartController[IO](cartService)

        when(cartService.removeItem(any[User.Id], any[Item.Id])).thenReturn(IO.unit)

        val request                    = Request[IO](uri = uri"/shopping-cart/607995e0-8e3a-11ea-bc55-0242ac130003", method = Method.DELETE)
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[Cart](response, Status.NoContent, None)
        verify(cartService).removeItem(authedUser.value.id, item1Id)
      }
    }

    "POST /shopping-cart" should {
      "add items to a shopping cart" in {
        val cartService = mockService
        val controller  = new CartController[IO](cartService)

        when(cartService.add(any[User.Id], any[Cart])).thenReturn(IO.unit)

        val request                    = Request[IO](uri = uri"/shopping-cart", method = Method.POST).withEntity(shoppingCartReq)
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[Cart](response, Status.Ok, None)
        verify(cartService).add(authedUser.value.id, cart)
      }
    }

    "PUT /shopping-cart" should {
      "update items to a shopping cart" in {
        val cartService = mockService
        val controller  = new CartController[IO](cartService)

        when(cartService.update(any[User.Id], any[Cart])).thenReturn(IO.unit)

        val request                    = Request[IO](uri = uri"/shopping-cart", method = Method.PUT).withEntity(shoppingCartReq)
        val response: IO[Response[IO]] = controller.routes(authMiddleware).orNotFound.run(request)

        verifyResponse[Cart](response, Status.Ok, None)
        verify(cartService).update(authedUser.value.id, cart)
      }
    }
  }

  def shoppingCartReq: Json = json"""{"items":[{"itemId":"607995e0-8e3a-11ea-bc55-0242ac130003","quantity":4}]}"""

  def mockService: CartService[IO] = mock[CartService[IO]]

}
