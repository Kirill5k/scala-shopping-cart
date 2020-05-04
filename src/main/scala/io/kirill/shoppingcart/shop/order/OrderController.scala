package io.kirill.shoppingcart.shop.order

import cats.effect.Sync
import cats.implicits._
import io.circe.generic.auto._
import io.kirill.shoppingcart.auth.CommonUser
import io.kirill.shoppingcart.common.errors.{EmptyCart, OrderNotFound}
import io.kirill.shoppingcart.common.json._
import io.kirill.shoppingcart.common.web.RestController
import io.kirill.shoppingcart.shop.cart.CartService
import io.kirill.shoppingcart.shop.item.ItemService
import io.kirill.shoppingcart.shop.payment.{Card, Payment, PaymentService}
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import squants.market.GBP

final class OrderController[F[_]: Sync](
    orderService: OrderService[F],
    cartService: CartService[F],
    itemService: ItemService[F],
    paymentService: PaymentService[F]
) extends RestController[F] {
  import OrderController._

  private val prefixPath = "/orders"

  private val httpRoutes: AuthedRoutes[CommonUser, F] =
    AuthedRoutes.of {
      case GET -> Root as user =>
        withErrorHandling {
          Ok(orderService.findBy(user.value.id))
        }
      case GET -> Root / UUIDVar(orderId) as user =>
        withErrorHandling {
          for {
            order <- orderService.get(user.value.id, OrderId(orderId))
            res <- order match {
              case None                                         => NotFound()
              case Some(order) if order.userId != user.value.id => Forbidden()
              case Some(order)                                  => Ok(order)
            }
          } yield res
        }
      case POST -> Root / "checkout" as user =>
        withErrorHandling {
          for {
            cart  <- cartService.get(user.value.id).ensure(EmptyCart)(_.items.nonEmpty)
            items <- cart.items.map(ci => itemService.findById(ci.item).map((_, ci.quantity))).toList.sequence
            orderItems = items.map { case (i, q) => OrderItem(i.id, i.price, q) }
            total      = items.foldLeft(GBP(0)) { case (total, (i, q)) => total + (i.price * q.value) }
            orderId <- orderService.create(OrderCheckout(user.value.id, orderItems, total))
            _       <- cartService.delete(user.value.id)
            res     <- Created(orderId)
          } yield res
        }
      case authedReq @ POST -> Root / UUIDVar(orderId) / "payment" as user =>
        withErrorHandling {
          for {
            paymentReq <- authedReq.req.as[OrderPaymentRequest]
            oid = OrderId(orderId)
            orderOpt <- orderService.get(user.value.id, oid).ensure(OrderNotFound(oid))(_.isDefined)
            order    <- orderOpt.fold(OrderNotFound(oid).raiseError[F, Order])(_.pure[F])
            pid      <- paymentService.process(Payment(order, paymentReq.card))
            _        <- orderService.update(OrderPayment(OrderId(orderId), pid))
            res      <- NoContent()
          } yield res
        }
    }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] =
    Router(prefixPath -> authMiddleware(httpRoutes))
}

object OrderController {
  final case class OrderPaymentRequest(card: Card)
}
