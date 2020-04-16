package io.kirill.shoppingcart.common.auth

import java.util.UUID

final case class UserId(value: UUID)     extends AnyVal
final case class UserName(value: String) extends AnyVal
final case class Password(value: String) extends AnyVal
final case class JwtToken(value: String) extends AnyVal

final case class User(
    id: UserId,
    name: UserName
)
