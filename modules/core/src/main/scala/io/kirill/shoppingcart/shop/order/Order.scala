package io.kirill.shoppingcart.shop.order

import io.estatico.newtype.macros.newtype
import io.kirill.shoppingcart.auth.user.User
import io.kirill.shoppingcart.shop.item.Item
import io.kirill.shoppingcart.shop.payment.Payment
import squants.market.Money

import java.util.UUID

final case class OrderItem(
    itemId: Item.Id,
    price: Money,
    quantity: Item.Quantity
)

final case class Order(
    id: Order.Id,
    status: Order.Status,
    userId: User.Id,
    paymentId: Option[Payment.Id],
    items: List[OrderItem],
    totalPrice: Money
)

final case class OrderCheckout(
    userId: User.Id,
    items: List[OrderItem],
    totalPrice: Money,
    status: Order.Status = Order.Status.AwaitingPayment
)

final case class OrderPayment(
    id: Order.Id,
    paymentId: Payment.Id,
    status: Order.Status = Order.Status.Processing
)

object Order {
  @newtype case class Id(value: UUID)
  @newtype case class Status(value: String)

  object Status {
    val AwaitingPayment = Status("order received. awaiting payment")
    val Processing      = Status("payment received. awaiting payment")
  }
}
