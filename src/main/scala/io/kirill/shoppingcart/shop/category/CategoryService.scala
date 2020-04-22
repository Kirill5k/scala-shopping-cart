package io.kirill.shoppingcart.shop.category

trait CategoryService[F[_]] {
  def findAll: F[Seq[Category]]
  def create(name: CategoryName): F[Unit]
}
