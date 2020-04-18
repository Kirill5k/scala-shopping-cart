package io.kirill.shoppingcart.auth

import java.util.UUID

final case class UserId(value: UUID)     extends AnyVal
final case class Username(value: String) extends AnyVal
final case class Password(value: String) extends AnyVal
final case class JwtToken(value: String) extends AnyVal

final case class User(
    id: UserId,
    name: Username,
    password: Password
)
