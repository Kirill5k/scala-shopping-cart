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

final class ItemRepository[F[_]: Sync] private (val sessionPool: Resource[F, Session[F]]) extends Repository[F] {
  import ItemRepository._

  def findAll: F[List[Item]] =
    run(_.execute(selectAll))
}

object ItemRepository {
  private val decoder: Decoder[Item] =
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

  private val selectAll: Query[Void, Item] =
    sql"""
         SELECT i.id, i.name, i.description, i.price, b.id, b.name, b.id, b.bame
         FROM items AS i
         INNER JOIN brands AS b ON i.brand_id = b.id
         INNER JOIN categories AS c ON i.category_id = c.id
         """.query(decoder)
}
