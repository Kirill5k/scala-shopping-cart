package io.kirill.shoppingcart.shop

import java.util.UUID

package object category {
  final case class CategoryId(value: UUID)     extends AnyVal
  final case class CategoryName(value: String) extends AnyVal

  final case class Category(id: CategoryId, name: CategoryName)

}
