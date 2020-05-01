package io.kirill.shoppingcart.shop

import java.util.UUID

import io.kirill.shoppingcart.auth.user.UserId
import io.kirill.shoppingcart.shop.cart.{Cart, Quantity}
import io.kirill.shoppingcart.shop.item.ItemId
import io.kirill.shoppingcart.shop.payment.PaymentId
import squants.market.Money

package object order {
  final case class OrderId(value: UUID) extends AnyVal

  final case class OrderItem(itemId: ItemId, price: Money, quantity: Quantity)

  final case class Order(
      id: OrderId,
      userId: UserId,
      paymentId: PaymentId,
      items: Seq[OrderItem],
      totalPrice: Money
  )

  final case class CreateOrder(
      userId: UserId,
      paymentId: PaymentId,
      items: Seq[OrderItem],
      totalPrice: Money
  )

  final case class OrderCheckout(
      userId: UserId,
      cart: Cart
  )
}
