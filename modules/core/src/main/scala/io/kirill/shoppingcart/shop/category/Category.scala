package io.kirill.shoppingcart.shop.category

import io.estatico.newtype.macros.newtype

import java.util.UUID

@newtype case class CategoryId(value: UUID)
@newtype case class CategoryName(value: String)

final case class Category(id: CategoryId, name: CategoryName)
