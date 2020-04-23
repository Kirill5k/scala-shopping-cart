package io.kirill.shoppingcart.shop.item

import java.util.UUID

import io.kirill.shoppingcart.shop.brand.{Brand, BrandId, BrandName}
import io.kirill.shoppingcart.shop.category.{Category, CategoryId, CategoryName}
import squants.market.{GBP, Money}

object ItemBuilder {

  def item(name: String): Item = {
    Item(
      ItemId(UUID.randomUUID()),
      ItemName(name),
      ItemDescription("test item"),
      Money(10, GBP),
      Brand(BrandId(UUID.randomUUID()), BrandName("Test Brand")),
      Category(CategoryId(UUID.randomUUID()), CategoryName("Test Category"))
    )
  }
}
