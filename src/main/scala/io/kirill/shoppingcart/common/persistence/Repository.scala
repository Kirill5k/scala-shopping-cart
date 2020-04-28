package io.kirill.shoppingcart.common.persistence

import cats.effect.{Resource, Sync}
import cats.implicits._
import io.kirill.shoppingcart.common.errors.SqlConstraintViolation
import skunk.{Session, SqlState}

trait Repository[F[_]] {
  protected def sessionPool: Resource[F, Session[F]]

  protected def run[A](command: Session[F] => F[A])(implicit S: Sync[F]): F[A] = {
    sessionPool.use(command).handleErrorWith {
      case SqlState.UniqueViolation(ex) =>
        S.raiseError(SqlConstraintViolation(ex.detail.fold(ex.message)(m => m)))
    }
  }
}
