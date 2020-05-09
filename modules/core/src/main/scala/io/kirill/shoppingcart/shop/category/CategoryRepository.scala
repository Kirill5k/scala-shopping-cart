package io.kirill.shoppingcart.shop.category

import java.util.UUID

import cats.effect.{Resource, Sync}
import cats.implicits._
import io.kirill.shoppingcart.common.persistence.Repository
import skunk._
import skunk.implicits._
import skunk.codec.all._

trait CategoryRepository[F[_]] extends Repository[F, Category] {
  def findAll: fs2.Stream[F, Category]
  def create(name: CategoryName): F[CategoryId]
}

final private class PostgresCategoryRepository[F[_]: Sync](
    val sessionPool: Resource[F, Session[F]]
) extends CategoryRepository[F] {
  import CategoryRepository._

  def findAll: fs2.Stream[F, Category] =
    fs2.Stream.evalSeq(run(_.execute(selectAll)))

  def create(name: CategoryName): F[CategoryId] =
    run { s =>
      s.prepare(insert).use { cmd =>
        val id = CategoryId(UUID.randomUUID())
        cmd.execute(Category(id, name)).map(_ => id)
      }
    }
}

object CategoryRepository {
  private[category] val codec: Codec[Category] =
    (uuid ~ varchar).imap {
      case i ~ n => Category(CategoryId(i), CategoryName(n))
    }(b => (b.id.value, b.name.value))

  private[category] val selectAll: Query[Void, Category] =
    sql"""
          SELECT * FROM categories
          """.query(codec)

  private[category] val insert: Command[Category] =
    sql"""
          INSERT INTO categories VALUES ($codec)
          """.command

  def make[F[_]: Sync](sessionPool: Resource[F, Session[F]]): F[CategoryRepository[F]] =
    Sync[F].delay(new PostgresCategoryRepository[F](sessionPool))
}
