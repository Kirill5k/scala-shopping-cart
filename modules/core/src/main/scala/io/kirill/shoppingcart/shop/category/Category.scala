package io.kirill.shoppingcart.shop.category

import io.estatico.newtype.macros.newtype

import java.util.UUID

final case class Category(id: Category.Id, name: Category.Name)

object Category {
  @newtype case class Id(value: UUID)
  @newtype case class Name(value: String)
}
