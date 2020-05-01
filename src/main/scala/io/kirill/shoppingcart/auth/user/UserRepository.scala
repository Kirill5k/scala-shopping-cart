package io.kirill.shoppingcart.auth.user

import java.util.UUID

import cats.effect.{Resource, Sync}
import cats.implicits._
import io.kirill.shoppingcart.common.persistence.Repository
import skunk._
import skunk.codec.all._
import skunk.implicits._

class UserRepository[F[_]: Sync] private (val sessionPool: Resource[F, Session[F]]) extends Repository[F] {
  import UserRepository._

  def findByName(username: Username): F[Option[User]] =
    run { session =>
      session.prepare(selectByName).use { ps =>
        ps.option(username.value)
      }
    }

  def create(username: Username, password: EncryptedPassword): F[UserId] =
    run { session =>
      session.prepare(insert).use { cmd =>
        val userId = UserId(UUID.randomUUID())
        cmd.execute(User(userId, username, password)).map(_ => userId)
      }
    }
}

object UserRepository {

  private val codec: Codec[User] =
    (uuid ~ varchar ~ varchar).imap {
      case i ~ n ~ p => User(UserId(i), Username(n), EncryptedPassword(p))
    }(u => u.id.value ~ u.name.value ~ u.password.value)

  private val selectByName: Query[String, User] =
    sql"""
         SELECT * FROM users
         WHERE name = $varchar
         """.query(codec)

  private val insert: Command[User] =
    sql"""
         INSERT INTO users
         VALUES ($codec)
         """.command

  def make[F[_]: Sync](sessionPool: Resource[F, Session[F]]): F[UserRepository[F]] =
    Sync[F].delay(new UserRepository[F](sessionPool))
}
