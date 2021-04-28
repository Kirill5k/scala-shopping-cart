package io.kirill.shoppingcart.shop.item

import java.util.UUID

import cats.effect.{Resource, Sync}
import cats.implicits._
import io.kirill.shoppingcart.common.persistence.Repository
import io.kirill.shoppingcart.shop.brand.{Brand}
import io.kirill.shoppingcart.shop.category.{Category}
import skunk._
import skunk.implicits._
import skunk.codec.all._
import squants.market.GBP

trait ItemRepository[F[_]] extends Repository[F, Item] {
  def findAll: fs2.Stream[F, Item]
  def findBy(brand: Brand.Name): fs2.Stream[F, Item]
  def find(id: Item.Id): F[Option[Item]]
  def create(item: CreateItem): F[Item.Id]
  def update(item: UpdateItem): F[Unit]
  def exists(id: Item.Id): F[Boolean]
}

final private class PostgresItemRepository[F[_]: Sync](val sessionPool: Resource[F, Session[F]]) extends ItemRepository[F] {
  import ItemRepository._

  def findAll: fs2.Stream[F, Item] =
    fs2.Stream.evalSeq(run(_.execute(selectAll)))

  def findBy(brand: Brand.Name): fs2.Stream[F, Item] =
    findManyBy(selectByBrand, brand.value)

  def find(id: Item.Id): F[Option[Item]] =
    findOneBy(selectById, id.value)

  def exists(id: Item.Id): F[Boolean] =
    run { session =>
      session.prepare(existsBy).use { cmd =>
        cmd.option(id.value).map(_.fold(false)(_ > 0))
      }
    }

  def create(item: CreateItem): F[Item.Id] =
    run { session =>
      session.prepare(insert).use { cmd =>
        val itemId = Item.Id(UUID.randomUUID())
        cmd.execute(itemId ~ item).map(_ => itemId)
      }
    }

  def update(item: UpdateItem): F[Unit] =
    runUpdateCommand(updatePrice, item)
}

object ItemRepository {
  private[item] val decoder: Decoder[Item] =
    (uuid ~ varchar ~ varchar ~ numeric ~ uuid ~ varchar ~ uuid ~ varchar).map {
      case i ~ n ~ d ~ p ~ bi ~ bn ~ ci ~ cn =>
        Item(
          Item.Id(i),
          Item.Name(n),
          Item.Description(d),
          GBP(p),
          Brand(Brand.Id(bi), Brand.Name(bn)),
          Category(Category.Id(ci), Category.Name(cn))
        )
    }

  private[item] val existsBy: Query[UUID, Long] =
    sql"""
         SELECT count(1)
         FROM items AS i
         WHERE i.id = $uuid
         """.query(int8)

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

  private[item] val insert: Command[Item.Id ~ CreateItem] =
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
