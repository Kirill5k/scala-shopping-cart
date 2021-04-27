package io.kirill.shoppingcart.shop.brand

import io.estatico.newtype.macros.newtype

import java.util.UUID

@newtype case class BrandId(value: UUID)
@newtype case class BrandName(value: String)

final case class Brand(id: BrandId, name: BrandName)
