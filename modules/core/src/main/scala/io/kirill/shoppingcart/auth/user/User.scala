package io.kirill.shoppingcart.auth.user

import io.estatico.newtype.macros.newtype

import java.util.UUID

@newtype case class UserId(value: UUID)
@newtype case class Username(value: String)
@newtype case class Password(value: String)
@newtype case class PasswordHash(value: String)

final case class User(
    id: UserId,
    name: Username,
    password: Option[PasswordHash]
)
