package io.kirill.shoppingcart.brand

trait BrandService[F[_]] {
  def findAll: F[Seq[Brand]]
  def create(name: BrandName): F[Unit]
}
