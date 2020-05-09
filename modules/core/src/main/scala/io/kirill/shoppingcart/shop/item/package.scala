package io.kirill.shoppingcart.shop

import java.util.UUID

import io.kirill.shoppingcart.shop.brand.{Brand, BrandId}
import io.kirill.shoppingcart.shop.category.{Category, CategoryId}
import squants.market.Money

package object item {
  final case class ItemId(value: UUID)            extends AnyVal
  final case class ItemName(value: String)        extends AnyVal
  final case class ItemDescription(value: String) extends AnyVal

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
}
