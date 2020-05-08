package io.kirill.shoppingcart.shop.category

import cats.effect.Sync

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
    categoryRepository.create(name)
}

object CategoryService {
  def make[F[_]: Sync](categoryRepository: CategoryRepository[F]): F[CategoryService[F]] =
    Sync[F].delay(new LiveCategoryService[F](categoryRepository))
}