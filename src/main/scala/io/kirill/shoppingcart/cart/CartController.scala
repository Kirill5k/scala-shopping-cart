package io.kirill.shoppingcart.cart

import cats.effect.Sync
import cats.implicits._
import io.circe.generic.auto._
import io.kirill.shoppingcart.common.auth.CommonUser
import io.kirill.shoppingcart.common.web.RestController
import io.kirill.shoppingcart.common.web.json._
import io.kirill.shoppingcart.item.ItemId
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.server.{AuthMiddleware, Router}

final class CartController[F[_]: Sync](cartService: CartService[F]) extends RestController[F] {
  import CartController._

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
      case authedReq @ PUT -> Root as user =>
        withErrorHandling {
          for {
            cart <- authedReq.req.as[CartUpdateRequest]
            _    <- cartService.update(user.value.id, cart.items)
            res  <- Ok()
          } yield res
        }
    }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] =
    Router(prefixPath -> authMiddleware(httpRoutes))
}

object CartController {
  final case class CartUpdateRequest(items: Map[ItemId, Quantity]) extends AnyVal
}
