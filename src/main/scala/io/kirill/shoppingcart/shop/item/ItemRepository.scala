package io.kirill.shoppingcart.shop.item

import java.util.UUID

import cats.effect.{Resource, Sync}
import cats.implicits._
import io.kirill.shoppingcart.common.persistence.Repository
import io.kirill.shoppingcart.shop.brand.{Brand, BrandId, BrandName}
import io.kirill.shoppingcart.shop.category.{Category, CategoryId, CategoryName}
import skunk._
import skunk.implicits._
import skunk.codec.all._
import squants.market.GBP

trait ItemRepository[F[_]] extends Repository[F, Item] {
  def findAll: fs2.Stream[F, Item]
  def findBy(brand: BrandName): fs2.Stream[F, Item]
  def find(id: ItemId): F[Option[Item]]
  def create(item: CreateItem): F[ItemId]
  def update(item: UpdateItem): F[Unit]
}

private final class PostgresItemRepository[F[_]: Sync] (val sessionPool: Resource[F, Session[F]]) extends ItemRepository[F] {
  import ItemRepository._

  def findAll: fs2.Stream[F, Item] =
    fs2.Stream.evalSeq(run(_.execute(selectAll)))

  def findBy(brand: BrandName): fs2.Stream[F, Item] =
    findManyBy(selectByBrand, brand.value)

  def find(id: ItemId): F[Option[Item]] =
    findOneBy(selectById, id.value)

  def create(item: CreateItem): F[ItemId] =
    run { session =>
      session.prepare(insert).use { cmd =>
        val itemId = ItemId(UUID.randomUUID())
        cmd.execute(itemId ~ item).map(_ => itemId)
      }
    }

  def update(item: UpdateItem): F[Unit] =
    run { session =>
      session.prepare(updatePrice).use { cmd =>
        cmd.execute(item).void
      }
    }
}

object ItemRepository {
  private[item] val decoder: Decoder[Item] =
    (uuid ~ varchar ~ varchar ~ numeric ~ uuid ~ varchar ~ uuid ~ varchar).map {
      case i ~ n ~ d ~ p ~ bi ~ bn ~ ci ~ cn =>
        Item(
          ItemId(i),
          ItemName(n),
          ItemDescription(d),
          GBP(p),
          Brand(BrandId(bi), BrandName(bn)),
          Category(CategoryId(ci), CategoryName(cn))
        )
    }

  private[item] val selectAll: Query[Void, Item] =
    sql"""
         SELECT i.id, i.name, i.description, i.price, b.id, b.name, c.id, c.name
         FROM items AS i
         INNER JOIN brands AS b ON i.brand_id = b.id
         INNER JOIN categories AS c ON i.category_id = c.id
         """.query(decoder)

  private[item] val selectByBrand: Query[String, Item] =
    sql"""
         SELECT i.id, i.name, i.description, i.price, b.id, b.name, c.id, c.name
         FROM items AS i
         INNER JOIN brands AS b ON i.brand_id = b.id
         INNER JOIN categories AS c ON i.category_id = c.id
         WHERE b.name LIKE ${varchar}
         """.query(decoder)

  private[item] val selectById: Query[UUID, Item] =
    sql"""
         SELECT i.id, i.name, i.description, i.price, b.id, b.name, c.id, c.name
         FROM items AS i
         INNER JOIN brands AS b ON i.brand_id = b.id
         INNER JOIN categories AS c ON i.category_id = c.id
         WHERE i.id = $uuid
         """.query(decoder)

  private[item] val insert: Command[ItemId ~ CreateItem] =
    sql"""
         INSERT INTO items
         VALUES ($uuid, $varchar, $varchar, $numeric, $uuid, $uuid)
         """.command.contramap {
      case id ~ i => id.value ~ i.name.value ~ i.description.value ~ i.price.amount ~ i.brandId.value ~ i.categoryId.value
    }

  private[item] val updatePrice: Command[UpdateItem] =
    sql"""
         UPDATE items
         SET price = $numeric
         WHERE id = $uuid
         """.command.contramap(i => i.price.amount ~ i.id.value)

  def make[F[_]: Sync](sessionPool: Resource[F, Session[F]]): F[ItemRepository[F]] =
    Sync[F].delay(new PostgresItemRepository[F](sessionPool))
}
