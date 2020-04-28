package io.kirill.shoppingcart.shop

import io.kirill.shoppingcart.shop.item.ItemId

package object cart {
  final case class Quantity(value: Int) extends AnyVal

  final case class CartItem(item: ItemId, quantity: Quantity)
  final case class Cart(items: Seq[CartItem])

  object Cart {
    def empty: Cart = Cart(Nil)
  }
}