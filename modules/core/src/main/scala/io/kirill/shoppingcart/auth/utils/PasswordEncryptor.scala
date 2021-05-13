package io.kirill.shoppingcart.auth.utils

import cats.effect.Sync
import cats.implicits._
import com.github.t3hnar.bcrypt._
import io.kirill.shoppingcart.auth.user.User
import io.kirill.shoppingcart.config.AuthConfig

trait PasswordEncryptor[F[_]] {
  def hash(password: User.Password): F[User.PasswordHash]
  def isValid(password: User.Password, passwordHash: User.PasswordHash): F[Boolean]
}

object PasswordEncryptor {

  def make[F[_]: Sync](config: AuthConfig): F[PasswordEncryptor[F]] =
    Sync[F].pure {
      new PasswordEncryptor[F] {
        override def hash(password: User.Password): F[User.PasswordHash] =
          Sync[F].delay(password.value.bcryptBounded(config.passwordSalt)).map(s => User.PasswordHash(s))

        override def isValid(password: User.Password, passwordHash: User.PasswordHash): F[Boolean] =
          Sync[F].fromTry(password.value.isBcryptedSafeBounded(passwordHash.value))
      }
    }
}
