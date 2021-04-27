package io.kirill.shoppingcart.shop.order

import io.estatico.newtype.macros.newtype

import java.util.UUID
import io.kirill.shoppingcart.auth.user.UserId
import io.kirill.shoppingcart.shop.cart.{Cart, Quantity}
import io.kirill.shoppingcart.shop.item.ItemId
import io.kirill.shoppingcart.shop.payment.PaymentId
import squants.market.Money

@newtype case class OrderId(value: UUID)
@newtype case class OrderStatus(value: String)

object OrderStatus {
  val awaitingPayment = OrderStatus("order received. awaiting payment")
  val processing      = OrderStatus("payment received. awaiting payment")
}

final case class OrderItem(itemId: ItemId, price: Money, quantity: Quantity)

final case class Order(
    id: OrderId,
    status: OrderStatus,
    userId: UserId,
    paymentId: Option[PaymentId],
    items: List[OrderItem],
    totalPrice: Money
)

final case class OrderCheckout(
    userId: UserId,
    items: List[OrderItem],
    totalPrice: Money,
    status: OrderStatus = OrderStatus.awaitingPayment
)

final case class OrderPayment(
    id: OrderId,
    paymentId: PaymentId,
    status: OrderStatus = OrderStatus.processing
)
