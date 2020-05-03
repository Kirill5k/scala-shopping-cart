package io.kirill.shoppingcart.shop.item

import cats.effect.Sync
import cats.implicits._
import io.kirill.shoppingcart.common.errors.ItemNotFound
import io.kirill.shoppingcart.shop.brand.BrandName

trait ItemService[F[_]] {
  def findAll: fs2.Stream[F, Item]
  def findBy(brand: BrandName): fs2.Stream[F, Item]
  def findById(id: ItemId): F[Item]
  def create(item: CreateItem): F[ItemId]
  def update(item: UpdateItem): F[Unit]
}

final private class LiveItemService[F[_]: Sync](
    itemRepository: ItemRepository[F]
) extends ItemService[F] {
  override def findAll: fs2.Stream[F, Item] =
    itemRepository.findAll

  override def findBy(brand: BrandName): fs2.Stream[F, Item] =
    itemRepository.findBy(brand)

  override def findById(id: ItemId): F[Item] =
    itemRepository.find(id).flatMap {
      case None => ItemNotFound(id).raiseError[F, Item]
      case Some(item) => item.pure[F]
    }

  override def create(item: CreateItem): F[ItemId] =
    itemRepository.create(item)

  override def update(item: UpdateItem): F[Unit] =
    itemRepository.update(item)
}

object ItemService {
  def make[F[_]: Sync](itemRepository: ItemRepository[F]): F[ItemService[F]] =
    Sync[F].delay(new LiveItemService[F](itemRepository))
}