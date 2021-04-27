package io.kirill.shoppingcart.auth

import cats.effect.Sync
import cats.implicits._
import io.kirill.shoppingcart.auth.user.{Password, PasswordHash}
import com.github.t3hnar.bcrypt._
import io.kirill.shoppingcart.config.{AppConfig, AuthConfig}

trait PasswordEncryptor[F[_]] {
  def hash(password: Password): F[PasswordHash]
  def isValid(password: Password, passwordHash: PasswordHash): F[Boolean]
}

object PasswordEncryptor {

  def make[F[_]: Sync](config: AuthConfig): F[PasswordEncryptor[F]] =
    Sync[F].delay {
      new PasswordEncryptor[F] {
        override def hash(password: Password): F[PasswordHash] =
          Sync[F].delay(password.value.bcryptBounded(config.passwordSalt)).map(PasswordHash)

        override def isValid(password: Password, passwordHash: PasswordHash): F[Boolean] =
          Sync[F].fromTry(password.value.isBcryptedSafeBounded(passwordHash.value))
      }
    }
}
