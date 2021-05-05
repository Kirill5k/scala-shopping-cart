package io.kirill.shoppingcart.shop.order

import io.kirill.shoppingcart.auth.user.User
import io.kirill.shoppingcart.shop.item.Item
import io.kirill.shoppingcart.shop.payment.Payment

import java.util.UUID
import squants.market.GBP

object OrderBuilder {

  def order(
      id: Order.Id = Order.Id(UUID.randomUUID()),
      userId: User.Id = User.Id(UUID.randomUUID())
  ): Order =
    Order(
      id,
      Order.Status.AwaitingPayment,
      userId,
      Some(Payment.Id(UUID.randomUUID())),
      List(OrderItem(Item.Id(UUID.randomUUID()), GBP(10), Item.Quantity(2))),
      GBP(20)
    )
}
