package io.kirill.shoppingcart.category

import java.util.UUID

final case class CategoryId(value: UUID)     extends AnyVal
final case class CategoryName(value: String) extends AnyVal

final case class Category(id: CategoryId, name: CategoryName)
