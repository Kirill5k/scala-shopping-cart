package io.kirill.shoppingcart.category

trait CategoryService[F[_]] {
  def findAll: F[Seq[Category]]
  def create(name: CategoryName): F[Unit]
}
