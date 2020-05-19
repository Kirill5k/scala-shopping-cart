package io.kirill.shoppingcart.shop.category

import cats.effect.Sync
import cats.implicits._
import io.kirill.shoppingcart.common.errors.{CategoryAlreadyExists, UniqueViolation}

trait CategoryService[F[_]] {
  def findAll: fs2.Stream[F, Category]
  def create(name: CategoryName): F[CategoryId]
}

final private class LiveCategoryService[F[_]: Sync](
    categoryRepository: CategoryRepository[F]
) extends CategoryService[F] {

  override def findAll: fs2.Stream[F, Category] =
    categoryRepository.findAll

  override def create(name: CategoryName): F[CategoryId] =
    categoryRepository.create(name).handleErrorWith {
      case UniqueViolation(_) => Sync[F].raiseError(CategoryAlreadyExists(name))
    }
}

object CategoryService {
  def make[F[_]: Sync](categoryRepository: CategoryRepository[F]): F[CategoryService[F]] =
    Sync[F].delay(new LiveCategoryService[F](categoryRepository))
}
