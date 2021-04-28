package io.kirill.shoppingcart.shop.item

import java.util.UUID

import io.kirill.shoppingcart.shop.brand.{Brand}
import io.kirill.shoppingcart.shop.category.{Category}
import squants.market.{GBP, Money}

object ItemBuilder {

  def item(name: String, price: Money = Money(10, GBP)): Item =
    Item(
      Item.Id(UUID.randomUUID()),
      Item.Name(name),
      Item.Description("test item"),
      price,
      Brand(Brand.Id(UUID.randomUUID()), Brand.Name("Test Brand")),
      Category(Category.Id(UUID.randomUUID()), Category.Name("Test Category"))
    )
}
