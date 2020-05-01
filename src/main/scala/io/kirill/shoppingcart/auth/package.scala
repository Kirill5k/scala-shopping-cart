package io.kirill.shoppingcart

import dev.profunktor.auth.jwt.JwtSymmetricAuth
import io.kirill.shoppingcart.auth.user.User
import javax.crypto.Cipher

package object auth {
  final case class PasswordSalt(value: String) extends AnyVal

  final case class EncryptCipher(value: Cipher) extends AnyVal
  final case class DecryptCipher(value: Cipher) extends AnyVal

  final case class AdminJwtAuth(value: JwtSymmetricAuth) extends AnyVal
  final case class UserJwtAuth(value: JwtSymmetricAuth)  extends AnyVal

  final case class CommonUser(value: User) extends AnyVal
  final case class AdminUser(value: User)  extends AnyVal
}
