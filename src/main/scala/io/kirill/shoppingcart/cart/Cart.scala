package io.kirill.shoppingcart.cart

import io.kirill.shoppingcart.item.Item
import squants.market.{GBP, Money}

final case class Quantity(value: Int) extends AnyVal

final case class CartItem(item: Item, quantity: Quantity)
final case class Cart(items: Seq[CartItem], total: Money)

object Cart {
  def empty: Cart = Cart(Nil, Money(0, GBP))
}
