package io.kirill.shoppingcart.shop.item

import io.kirill.shoppingcart.brand.BrandName

trait ItemService[F[_]] {
  def findAll: F[Seq[Item]]
  def findBy(brand: BrandName): F[Seq[Item]]
  def findById(id: ItemId): F[Item]
  def create(item: CreateItem): F[ItemId]
  def update(item: UpdateItem): F[Unit]
}
