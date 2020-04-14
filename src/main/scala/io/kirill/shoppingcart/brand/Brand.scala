package io.kirill.shoppingcart.brand

import java.util.UUID

final case class BrandId(value: UUID)     extends AnyVal
final case class BrandName(value: String) extends AnyVal

final case class Brand(id: BrandId, name: BrandName)
