package io.kirill.shoppingcart.shop

import java.util.UUID

package object brand {
  final case class BrandId(value: UUID)     extends AnyVal
  final case class BrandName(value: String) extends AnyVal

  final case class Brand(id: BrandId, name: BrandName)
}