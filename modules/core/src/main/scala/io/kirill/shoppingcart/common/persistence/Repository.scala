package io.kirill.shoppingcart.common.persistence

import cats.effect.{Resource, Sync}
import cats.implicits._
import io.kirill.shoppingcart.common.errors.{ForeignKeyViolation, UniqueViolation}
import skunk.{Command, Query, Session, SqlState}

trait Repository[F[_], E] {
  protected def sessionPool: Resource[F, Session[F]]

  protected def run[A](command: Session[F] => F[A])(implicit F: Sync[F]): F[A] =
    sessionPool.use(command).handleErrorWith {
      case SqlState.ForeignKeyViolation(ex) =>
        ForeignKeyViolation(ex.detail.fold(ex.message)(m => m)).raiseError[F, A]
      case SqlState.UniqueViolation(ex) =>
        UniqueViolation(ex.detail.fold(ex.message)(m => m)).raiseError[F, A]
    }

  protected def findOneBy[A](command: Query[A, E], arg: A)(implicit F: Sync[F]): F[Option[E]] =
    run { session =>
      session.prepare(command).use { ps =>
        ps.option(arg)
      }
    }

  protected def findManyBy[A](command: Query[A, E], arg: A)(implicit F: Sync[F]): fs2.Stream[F, E] =
    for {
      s  <- fs2.Stream.resource(sessionPool)
      ps <- fs2.Stream.resource(s.prepare(command))
      x  <- ps.stream(arg, 1024)
    } yield x

  protected def runUpdateCommand[A](command: Command[A], arg: A)(implicit F: Sync[F]): F[Unit] =
    run { session =>
      session.prepare(command).use { cmd =>
        cmd.execute(arg).void
      }
    }
}
