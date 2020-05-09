package io.kirill.shoppingcart.shop.cart

import java.util.UUID

import cats.effect.Sync
import cats.implicits._
import io.circe.generic.auto._
import io.kirill.shoppingcart.auth.CommonUser
import io.kirill.shoppingcart.common.web.RestController
import io.kirill.shoppingcart.common.json._
import io.kirill.shoppingcart.shop.item.ItemId
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}

final class CartController[F[_]: Sync](cartService: CartService[F]) extends RestController[F] {
  private val prefixPath = "/shopping-cart"

  private val httpRoutes: AuthedRoutes[CommonUser, F] =
    AuthedRoutes.of {
      case DELETE -> Root / UUIDVar(itemId) as user =>
        withErrorHandling {
          cartService.removeItem(user.value.id, ItemId(itemId)) *> NoContent()
        }
      case GET -> Root as user =>
        withErrorHandling {
          Ok(cartService.get(user.value.id))
        }
      case authedReq @ POST -> Root as user =>
        withErrorHandling {
          for {
            cart <- authedReq.req.as[Cart]
            _    <- cartService.add(user.value.id, cart)
            res  <- Ok()
          } yield res
        }
      case authedReq @ PUT -> Root as user =>
        withErrorHandling {
          for {
            cart <- authedReq.req.as[Cart]
            _    <- cartService.update(user.value.id, cart)
            res  <- Ok()
          } yield res
        }
    }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] =
    Router(prefixPath -> authMiddleware(httpRoutes))
}

object CartController {
}
