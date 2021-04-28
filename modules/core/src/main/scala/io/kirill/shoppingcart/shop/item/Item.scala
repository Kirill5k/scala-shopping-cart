package io.kirill.shoppingcart.shop.item

import io.estatico.newtype.macros.newtype

import java.util.UUID
import io.kirill.shoppingcart.shop.brand.Brand
import io.kirill.shoppingcart.shop.category.Category
import squants.market.Money

final case class Item(
    id: Item.Id,
    name: Item.Name,
    description: Item.Description,
    price: Money,
    brand: Brand,
    category: Category
)

final case class CreateItem(
    name: Item.Name,
    description: Item.Description,
    price: Money,
    brandId: Brand.Id,
    categoryId: Category.Id
)

final case class UpdateItem(
    id: Item.Id,
    price: Money
)

object Item {
  @newtype case class Id(value: UUID)
  @newtype case class Name(value: String)
  @newtype case class Description(value: String)
  @newtype case class Quantity(value: Int)
}
