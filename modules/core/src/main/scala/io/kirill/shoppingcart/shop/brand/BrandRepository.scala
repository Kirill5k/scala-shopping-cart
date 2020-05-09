package io.kirill.shoppingcart.shop.brand

import java.util.UUID

import cats.effect.{Resource, Sync}
import cats.implicits._
import io.kirill.shoppingcart.common.persistence.Repository
import skunk._
import skunk.implicits._
import skunk.codec.all._

trait BrandRepository[F[_]] extends Repository[F, Brand] {
  def findAll: fs2.Stream[F, Brand]
  def create(name: BrandName): F[BrandId]
}

final private class PostgresBrandRepository[F[_]: Sync](
    val sessionPool: Resource[F, Session[F]]
) extends BrandRepository[F] {
  import BrandRepository._

  def findAll: fs2.Stream[F, Brand] =
    fs2.Stream.evalSeq(run(_.execute(selectAll)))

  def create(name: BrandName): F[BrandId] =
    run { s =>
      s.prepare(insert).use { cmd =>
        val brandId = BrandId(UUID.randomUUID())
        cmd.execute(Brand(brandId, name)).map(_ => brandId)
      }
    }
}

object BrandRepository {
  private[brand] val codec: Codec[Brand] =
    (uuid ~ varchar).imap {
      case i ~ n => Brand(BrandId(i), BrandName(n))
    }(b => (b.id.value, b.name.value))

  private[brand] val selectAll: Query[Void, Brand] =
    sql"""
          SELECT * FROM brands
          """.query(codec)

  private[brand] val insert: Command[Brand] =
    sql"""
          INSERT INTO brands VALUES ($codec)
          """.command

  def make[F[_]: Sync](sessionPool: Resource[F, Session[F]]): F[BrandRepository[F]] =
    Sync[F].delay(new PostgresBrandRepository[F](sessionPool))
}
