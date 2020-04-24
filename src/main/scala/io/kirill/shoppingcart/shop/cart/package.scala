package io.kirill.shoppingcart.shop

import io.kirill.shoppingcart.shop.item.Item
import squants.market.{GBP, Money}

package object cart {
  final case class Quantity(value: Int) extends AnyVal

  final case class CartItem(item: Item, quantity: Quantity)
  final case class Cart(items: Seq[CartItem], total: Money)

  object Cart {
    def empty: Cart = Cart(Nil, Money(0, GBP))
  }
}