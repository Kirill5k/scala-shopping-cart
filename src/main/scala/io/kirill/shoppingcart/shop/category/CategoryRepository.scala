package io.kirill.shoppingcart.shop.category

import java.util.UUID

import cats.effect.{Resource, Sync}
import cats.implicits._
import io.kirill.shoppingcart.common.persistence.Repository
import skunk._
import skunk.implicits._
import skunk.codec.all._

final class CategoryRepository[F[_]: Sync] private(val sessionPool: Resource[F, Session[F]]) extends Repository[F] {
  import CategoryRepository._

  def findAll: F[List[Category]] =
    run(_.execute(selectAll))

  def create(name: CategoryName): F[Category] =
    run { s =>
      s.prepare(insert).use { cmd =>
        for {
          b <- Sync[F].pure(Category(CategoryId(UUID.randomUUID()), name))
          _ <- cmd.execute(b).void
        } yield b
      }
    }
}

object CategoryRepository {
  private val codec: Codec[Category] =
    (uuid ~ varchar).imap {
      case i ~ n => Category(CategoryId(i), CategoryName(n))
    }(b => (b.id.value, b.name.value))

  private val selectAll: Query[Void, Category] =
    sql"""
          SELECT * FROM categories
          """.query(codec)

  private val insert: Command[Category] =
    sql"""
          INSERT INTO categories VALUES ($codec)
          """.command

  def make[F[_]: Sync](sessionPool: Resource[F, Session[F]]): F[CategoryRepository[F]] =
    Sync[F].delay(new CategoryRepository[F](sessionPool))
}

