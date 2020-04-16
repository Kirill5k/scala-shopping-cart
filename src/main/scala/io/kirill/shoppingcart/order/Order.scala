package io.kirill.shoppingcart.order

import java.util.UUID

import io.kirill.shoppingcart.cart.{Cart, Quantity}
import io.kirill.shoppingcart.common.auth.UserId
import io.kirill.shoppingcart.item.ItemId
import io.kirill.shoppingcart.payment.PaymentId
import squants.market.Money

final case class OrderId(value: UUID)   extends AnyVal

final case class Order(
    id: OrderId,
    userId: UserId,
    paymentId: PaymentId,
    items: Map[ItemId, Quantity],
    total: Money
)

final case class CreateOrder(
    userId: UserId,
    paymentId: PaymentId,
    cart: Cart
)
