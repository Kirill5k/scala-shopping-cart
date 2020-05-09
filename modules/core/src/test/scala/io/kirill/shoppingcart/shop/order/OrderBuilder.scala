package io.kirill.shoppingcart.shop.order

import java.util.UUID

import io.kirill.shoppingcart.auth.user.UserId
import io.kirill.shoppingcart.shop.cart.Quantity
import io.kirill.shoppingcart.shop.item.ItemId
import io.kirill.shoppingcart.shop.payment.PaymentId
import squants.market.GBP

object OrderBuilder {

  def order: Order =
    Order(
      OrderId(UUID.randomUUID()),
      OrderStatus.awaitingPayment,
      UserId(UUID.randomUUID()),
      Some(PaymentId(UUID.randomUUID())),
      List(OrderItem(ItemId(UUID.randomUUID()), GBP(10), Quantity(2))),
      GBP(20)
    )
}
