package io.kirill.shoppingcart.shop.item

import io.estatico.newtype.macros.newtype

import java.util.UUID
import io.kirill.shoppingcart.shop.brand.{Brand, BrandId}
import io.kirill.shoppingcart.shop.category.{Category, CategoryId}
import squants.market.Money

@newtype case class ItemId(value: UUID)
@newtype case class ItemName(value: String)
@newtype case class ItemDescription(value: String)

final case class Item(
    id: ItemId,
    name: ItemName,
    description: ItemDescription,
    price: Money,
    brand: Brand,
    category: Category
)

final case class CreateItem(
    name: ItemName,
    description: ItemDescription,
    price: Money,
    brandId: BrandId,
    categoryId: CategoryId
)

final case class UpdateItem(
    id: ItemId,
    price: Money
)
