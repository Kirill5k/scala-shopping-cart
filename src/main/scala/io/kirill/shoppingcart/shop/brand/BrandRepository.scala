package io.kirill.shoppingcart.shop.brand

import java.util.UUID

import cats.effect.{Resource, Sync}
import cats.implicits._
import io.kirill.shoppingcart.common.persistence.Repository
import skunk._
import skunk.implicits._
import skunk.codec.all._

final class BrandRepository[F[_]: Sync] private(val sessionPool: Resource[F, Session[F]]) extends Repository[F] {
  import BrandRepository._

  def findAll: F[List[Brand]] =
    run(_.execute(selectAll))

  def create(name: BrandName): F[Brand] =
    run { s =>
      s.prepare(insert).use { cmd =>
        for {
          b <- Sync[F].pure(Brand(BrandId(UUID.randomUUID()), name))
          _ <- cmd.execute(b).void
        } yield b
      }
    }
}

object BrandRepository {
  private val codec: Codec[Brand] =
    (uuid ~ varchar).imap {
      case i ~ n => Brand(BrandId(i), BrandName(n))
    }(b => (b.id.value, b.name.value))

  private val selectAll: Query[Void, Brand] =
    sql"""
          SELECT * FROM brands
          """.query(codec)

  private val insert: Command[Brand] =
    sql"""
          INSERT INTO brands VALUES ($codec)
          """.command

  def apply[F[_]: Sync](implicit sessionPool: Resource[F, Session[F]]): BrandRepository[F] =
    new BrandRepository[F](sessionPool)
}
