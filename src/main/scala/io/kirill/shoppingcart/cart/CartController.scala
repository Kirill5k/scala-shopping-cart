package io.kirill.shoppingcart.cart

import cats.effect.Sync
import cats.{Defer, Monad}
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.kirill.shoppingcart.cart.CartController.CartUpdateRequest
import io.kirill.shoppingcart.common.security.user.CommonUser
import io.kirill.shoppingcart.common.json._
import io.kirill.shoppingcart.item.ItemId
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final class CartController[F[_]: Defer: Sync](cartService: CartService[F]) extends Http4sDsl[F] {
  private val prefixPath = "/shopping-cart"

  private val httpRoutes: AuthedRoutes[CommonUser, F] =
    AuthedRoutes.of {
      case DELETE -> Root / UUIDVar(itemId) as user =>
        cartService.removeItem(user.value.id, ItemId(itemId)) *> NoContent()
      case GET -> Root as user =>
        Ok(cartService.get(user.value.id))
      case authedReq @ PUT -> Root as user =>
        for {
          cart <- authedReq.req.as[CartUpdateRequest]
          _    <- cartService.update(user.value.id, cart.items)
        } yield Ok()
    }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] =
    Router(prefixPath -> authMiddleware(httpRoutes))
}

object CartController {
  final case class CartUpdateRequest(items: Map[ItemId, Quantity]) extends AnyVal
}
