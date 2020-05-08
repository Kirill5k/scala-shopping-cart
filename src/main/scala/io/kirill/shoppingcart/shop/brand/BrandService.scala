package io.kirill.shoppingcart.shop.brand

import cats.effect.Sync

trait BrandService[F[_]] {
  def findAll: fs2.Stream[F, Brand]
  def create(name: BrandName): F[BrandId]
}

final private class LiveBrandService[F[_]: Sync](
    brandRepository: BrandRepository[F]
) extends BrandService[F] {
  override def findAll: fs2.Stream[F, Brand] =
    brandRepository.findAll

  override def create(name: BrandName): F[BrandId] =
    brandRepository.create(name)
}

object BrandService {
  def make[F[_]: Sync](brandRepository: BrandRepository[F]): F[BrandService[F]] =
    Sync[F].delay(new LiveBrandService[F](brandRepository))
}
