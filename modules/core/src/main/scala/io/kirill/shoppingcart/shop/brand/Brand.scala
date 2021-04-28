package io.kirill.shoppingcart.shop.brand

import io.estatico.newtype.macros.newtype

import java.util.UUID

final case class Brand(id: Brand.Id, name: Brand.Name)

object Brand {
  @newtype case class Id(value: UUID)
  @newtype case class Name(value: String)
}
