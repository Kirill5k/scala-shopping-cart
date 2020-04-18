package io.kirill.shoppingcart

import dev.profunktor.auth.jwt.JwtSymmetricAuth

package object auth {
  final case class AdminJwtAuth(value: JwtSymmetricAuth) extends AnyVal
  final case class UserJwtAuth(value: JwtSymmetricAuth)  extends AnyVal

  final case class CommonUser(value: User) extends AnyVal
  final case class AdminUser(value: User)  extends AnyVal
}
