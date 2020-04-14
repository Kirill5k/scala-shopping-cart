package io.kirill.shoppingcart.cart

import java.util.UUID

import io.kirill.shoppingcart.item.{Item, ItemId}
import squants.market.Money

final case class CartId(value: UUID)  extends AnyVal
final case class Quantity(value: Int) extends AnyVal

final case class Cart(items: Map[ItemId, Quantity]) extends AnyVal

final case class CartItem(item: Item, quantity: Quantity)
final case class CartTotal(items: Seq[CartItem], total: Money)
