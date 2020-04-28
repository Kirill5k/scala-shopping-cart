package io.kirill.shoppingcart.shop

import java.util.UUID

import io.kirill.shoppingcart.auth.UserId
import io.kirill.shoppingcart.shop.cart.{Cart, Quantity}
import io.kirill.shoppingcart.shop.item.ItemId
import io.kirill.shoppingcart.shop.payment.PaymentId
import squants.market.Money

package object order {
  final case class OrderId(value: UUID) extends AnyVal

  final case class Order(
      id: OrderId,
      userId: UserId,
      paymentId: PaymentId,
      items: Seq[(ItemId, Quantity)],
      total: Money
  )

  final case class OrderCheckout(
      userId: UserId,
      cart: Cart
  )
}
