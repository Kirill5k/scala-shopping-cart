package io.kirill.shoppingcart.shop.brand

import cats.effect.Sync
import cats.implicits._
import io.kirill.shoppingcart.common.errors.{BrandAlreadyExists, UniqueViolation}

trait BrandService[F[_]] {
  def findAll: fs2.Stream[F, Brand]
  def create(name: Brand.Name): F[Brand.Id]
}

final private class LiveBrandService[F[_]: Sync](
    brandRepository: BrandRepository[F]
) extends BrandService[F] {
  override def findAll: fs2.Stream[F, Brand] =
    brandRepository.findAll

  override def create(name: Brand.Name): F[Brand.Id] =
    brandRepository.create(name).handleErrorWith {
      case UniqueViolation(_) => Sync[F].raiseError(BrandAlreadyExists(name))
    }
}

object BrandService {
  def make[F[_]: Sync](brandRepository: BrandRepository[F]): F[BrandService[F]] =
    Sync[F].delay(new LiveBrandService[F](brandRepository))
}
