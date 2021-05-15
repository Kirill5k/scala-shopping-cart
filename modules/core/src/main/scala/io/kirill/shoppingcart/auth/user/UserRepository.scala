package io.kirill.shoppingcart.auth.user

import cats.Monad

import java.util.UUID
import cats.effect.{Resource, Sync}
import cats.implicits._
import io.kirill.shoppingcart.common.persistence.Repository
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait UserRepository[F[_]] extends Repository[F, User] {
  def findByName(username: User.Name): F[Option[User]]
  def create(username: User.Name, password: User.PasswordHash): F[User.Id]
}

final private class LiveUserRepository[F[_]: Sync](
    val sessionPool: Resource[F, Session[F]]
) extends UserRepository[F] {
  import UserRepository._

  override def findByName(username: User.Name): F[Option[User]] =
    findOneBy(selectByName, username.value)

  override def create(username: User.Name, password: User.PasswordHash): F[User.Id] =
    run { session =>
      session.prepare(insert).use { cmd =>
        val userId = User.Id(UUID.randomUUID())
        cmd.execute(User(userId, username, Some(password))).map(_ => userId)
      }
    }
}

object UserRepository {

  private val codec: Codec[User] =
    (uuid ~ varchar ~ varchar.opt).imap {
      case i ~ n ~ p => User(User.Id(i), User.Name(n), p.map(User.PasswordHash.apply))
    }(u => u.id.value ~ u.name.value ~ u.password.map(_.value))

  val selectByName: Query[String, User] =
    sql"""
         SELECT * FROM users
         WHERE name = $varchar
         """.query(codec)

  val insert: Command[User] =
    sql"""
         INSERT INTO users
         VALUES ($codec)
         """.command

  def make[F[_]: Sync](sessionPool: Resource[F, Session[F]]): F[UserRepository[F]] =
    Monad[F].pure(new LiveUserRepository[F](sessionPool))
}
