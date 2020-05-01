package io.kirill.shoppingcart.auth

import cats.effect.Sync
import cats.implicits._
import io.kirill.shoppingcart.auth.user.{PasswordHash, Password}
import com.github.t3hnar.bcrypt._

trait PasswordEncrypter[F[_]] {
  def hash(password: Password): F[PasswordHash]
  def isValid(password: Password, passwordHash: PasswordHash): F[Boolean]
}

object PasswordEncrypter {

  def apply[F[_]: Sync](salt: Option[PasswordSalt] = None): PasswordEncrypter[F] =
    new PasswordEncrypter[F] {
      override def hash(password: Password): F[PasswordHash] =
        Sync[F].delay(salt.fold(password.value.bcrypt)(s => password.value.bcrypt(s.value))).map(PasswordHash)

      override def isValid(password: Password, passwordHash: PasswordHash): F[Boolean] =
        Sync[F].fromTry(password.value.isBcryptedSafe(passwordHash.value))
    }
}
