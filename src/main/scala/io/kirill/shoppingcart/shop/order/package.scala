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

  final case class OrderStatus(value: String) extends AnyVal
  object OrderStatus {
    val awaitingPayment = OrderStatus("order received. awaiting payment")
    val processing = OrderStatus("payment received. awaiting payment")
  }

  final case class Order(
      id: OrderId,
      status: OrderStatus,
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

  final case class OrderPayment(
      orderId: OrderId,
      paymentId: PaymentId
  )
}
