package io.kirill.shoppingcart.common.security

import java.util.UUID

import dev.profunktor.auth.jwt.JwtSymmetricAuth

object user {
  final case class AdminJwtAuth(value: JwtSymmetricAuth) extends AnyVal
  final case class UserJwtAuth(value: JwtSymmetricAuth)  extends AnyVal

  final case class UserId(value: UUID)     extends AnyVal
  final case class UserName(value: String) extends AnyVal
  final case class Password(value: String) extends AnyVal
  final case class JwtToken(value: String) extends AnyVal

  final case class User(
      id: UserId,
      name: UserName
  )

  final case class CommonUser(value: User) extends AnyVal
  final case class AdminUser(value: User)  extends AnyVal
}
