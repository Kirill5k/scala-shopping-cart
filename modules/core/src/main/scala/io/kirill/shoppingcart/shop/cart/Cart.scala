package io.kirill.shoppingcart.shop.cart

import io.kirill.shoppingcart.shop.item.Item

final case class CartItem(itemId: Item.Id, quantity: Item.Quantity)
final case class Cart(items: List[CartItem])

object Cart {
  val empty: Cart = Cart(Nil)
}