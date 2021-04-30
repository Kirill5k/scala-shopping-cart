package io.kirill.shoppingcart.shop.item

import cats.effect.Sync
import cats.implicits._
import io.kirill.shoppingcart.common.errors.ItemNotFound
import io.kirill.shoppingcart.shop.brand.Brand

trait ItemService[F[_]] {
  def findAll: fs2.Stream[F, Item]
  def findBy(brand: Brand.Name): fs2.Stream[F, Item]
  def findById(id: Item.Id): F[Item]
  def create(item: CreateItem): F[Item.Id]
  def update(item: UpdateItem): F[Unit]
}

final private class LiveItemService[F[_]: Sync](
    itemRepository: ItemRepository[F]
) extends ItemService[F] {
  override def findAll: fs2.Stream[F, Item] =
    itemRepository.findAll

  override def findBy(brand: Brand.Name): fs2.Stream[F, Item] =
    itemRepository.findBy(brand)

  override def findById(id: Item.Id): F[Item] =
    itemRepository.find(id).flatMap {
      case None       => ItemNotFound(id).raiseError[F, Item]
      case Some(item) => item.pure[F]
    }

  override def create(item: CreateItem): F[Item.Id] =
    itemRepository.create(item)

  override def update(item: UpdateItem): F[Unit] =
    itemRepository.exists(item.id).flatMap {
      case false => ItemNotFound(item.id).raiseError[F, Unit]
      case true  => itemRepository.update(item)
    }
}

object ItemService {
  def make[F[_]: Sync](itemRepository: ItemRepository[F]): F[ItemService[F]] =
    Sync[F].delay(new LiveItemService[F](itemRepository))
}
