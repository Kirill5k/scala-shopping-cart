package io.kirill.shoppingcart.order

import cats.effect.Sync
import cats.implicits._
import io.circe.generic.auto._
import io.kirill.shoppingcart.cart.CartService
import io.kirill.shoppingcart.auth.CommonUser
import io.kirill.shoppingcart.common.web.RestController
import io.kirill.shoppingcart.common.web.json._
import io.kirill.shoppingcart.payment.{Card, Payment, PaymentService}
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe._
import org.http4s.server.{AuthMiddleware, Router}

final class OrderController[F[_]: Sync](
    orderService: OrderService[F],
    cartService: CartService[F],
    paymentService: PaymentService[F]
) extends RestController[F] {
  import OrderController._

  private val prefixPath = "/orders"

  private val httpRoutes: AuthedRoutes[CommonUser, F] =
    AuthedRoutes.of {
      case GET -> Root as user =>
        Ok(orderService.findBy(user.value.id))
      case GET -> Root / UUIDVar(orderId) as user =>
        for {
          order <- orderService.get(OrderId(orderId))
          res <- order match {
            case None                                         => NotFound()
            case Some(order) if order.userId != user.value.id => Forbidden()
            case Some(order)                                  => Ok(order)
          }
        } yield res
      case authedReq @ POST -> Root / "checkout" as user =>
        for {
          checkoutReq <- authedReq.req.as[OrderCheckoutRequest]
          userId = user.value.id
          cart      <- cartService.get(userId)
          paymentId <- paymentService.process(Payment(userId, cart.total, checkoutReq.card))
          orderId   <- orderService.create(CreateOrder(userId, paymentId, cart))
          _         <- cartService.delete(userId)
          res       <- Created(orderId)
        } yield res
    }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] =
    Router(prefixPath -> authMiddleware(httpRoutes))
}

object OrderController {
  final case class OrderCheckoutRequest(card: Card)
}
