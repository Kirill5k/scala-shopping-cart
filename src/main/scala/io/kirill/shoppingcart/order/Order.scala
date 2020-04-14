package io.kirill.shoppingcart.order

import java.util.UUID

import io.kirill.shoppingcart.cart.{CartTotal, Quantity}
import io.kirill.shoppingcart.item.ItemId
import io.kirill.shoppingcart.payment.PaymentId
import io.kirill.shoppingcart.user.UserId
import squants.market.Money

final case class OrderId(value: UUID)   extends AnyVal

final case class Order(
    id: OrderId,
    paymentId: PaymentId,
    items: Map[ItemId, Quantity],
    total: Money
)

final case class CreateOrder(
    userId: UserId,
    paymentId: PaymentId,
    cart: CartTotal
)
