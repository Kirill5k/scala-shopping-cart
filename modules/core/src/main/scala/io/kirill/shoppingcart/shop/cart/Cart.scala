package io.kirill.shoppingcart.shop.cart

import io.estatico.newtype.macros.newtype
import io.kirill.shoppingcart.shop.item.ItemId
import squants.market.{GBP, Money}

@newtype case class Quantity(value: Int)

final case class CartItem(itemId: ItemId, quantity: Quantity)
final case class Cart(items: List[CartItem], total: Money)

object Cart {
  val empty: Cart = Cart(Nil, GBP(0))
}