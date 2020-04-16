package io.kirill.shoppingcart.order

import cats.effect.Sync
import cats.implicits._
import io.circe.generic.auto._
import io.kirill.shoppingcart.common.security.user.CommonUser
import io.kirill.shoppingcart.common.json._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final class OrderController[F[_]: Sync](orderService: OrderService[F]) extends Http4sDsl[F] {
  private val prefixPath = "/orders"

  private val httpRoutes: AuthedRoutes[CommonUser, F] =
    AuthedRoutes.of {
      case GET -> Root as user =>
        Ok(orderService.findBy(user.value.id))
      case GET -> Root / UUIDVar(orderId) as user =>
        for {
          order <- orderService.get(OrderId(orderId))
          res <- order match {
            case None => NotFound()
            case Some(order) if order.userId != user.value.id => Forbidden()
            case Some(order) => Ok(order)
          }
        } yield res
    }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] =
    Router(prefixPath -> authMiddleware(httpRoutes))
}
