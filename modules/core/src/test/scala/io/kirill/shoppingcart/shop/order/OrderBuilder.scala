package io.kirill.shoppingcart.shop.order

import java.util.UUID

import squants.market.GBP

object OrderBuilder {

  def order: Order =
    Order(
      Order.Id(UUID.randomUUID()),
      Order.Status.awaitingPayment,
      User.Id(UUID.randomUUID()),
      Some(Payment.Id(UUID.randomUUID())),
      List(OrderItem(Item.Id(UUID.randomUUID()), GBP(10), Quantity(2))),
      GBP(20)
    )
}
