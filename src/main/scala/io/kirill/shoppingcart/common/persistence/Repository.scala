package io.kirill.shoppingcart.common.persistence

import cats.effect.{Resource, Sync}
import cats.implicits._
import io.kirill.shoppingcart.common.errors.{ForeignKeyViolation, SqlConstraintViolation}
import skunk.{Query, Session, SqlState}

trait Repository[F[_], E] {
  protected def sessionPool: Resource[F, Session[F]]

  protected def run[A](command: Session[F] => F[A])(implicit S: Sync[F]): F[A] = {
    sessionPool.use(command).handleErrorWith {
      case SqlState.ForeignKeyViolation(ex) =>
        S.raiseError(ForeignKeyViolation(ex.detail.fold(ex.message)(m => m)))
      case SqlState.UniqueViolation(ex) =>
        S.raiseError(SqlConstraintViolation(ex.detail.fold(ex.message)(m => m)))
    }
  }

  protected def findOneBy[A](command: Query[A, E], arg: A)(implicit S: Sync[F]): F[Option[E]] =
    run { session =>
      session.prepare(command).use { ps =>
        ps.option(arg)
      }
    }

  protected def findManyBy[A](command: Query[A, E], arg: A)(implicit S: Sync[F]): F[List[E]] =
    run { session =>
      session.prepare(command).use { ps =>
        ps.stream(arg, 1024).compile.toList
      }
    }
}
