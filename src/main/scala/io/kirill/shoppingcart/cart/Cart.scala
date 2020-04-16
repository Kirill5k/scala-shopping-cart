package io.kirill.shoppingcart.cart

import java.util.UUID

import io.kirill.shoppingcart.item.Item
import squants.market.Money

final case class Quantity(value: Int) extends AnyVal

final case class CartItem(item: Item, quantity: Quantity)
final case class Cart(items: Seq[CartItem], total: Money)
