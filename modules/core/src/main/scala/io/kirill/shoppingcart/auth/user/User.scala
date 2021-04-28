package io.kirill.shoppingcart.auth.user

import io.estatico.newtype.macros.newtype

import java.util.UUID

final case class User(
    id: User.Id,
    name: User.Name,
    password: Option[User.PasswordHash]
)

object User {
  @newtype case class Id(value: UUID)
  @newtype case class Name(value: String)
  @newtype case class Password(value: String)
  @newtype case class PasswordHash(value: String)
}
