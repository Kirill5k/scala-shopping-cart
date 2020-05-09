package io.kirill.shoppingcart.auth.user

import java.util.UUID

object UserBuilder {

  def user(name: String = "Boris", passwordHash: String = "password-hash"): User =
    User(UserId(UUID.randomUUID()), Username(name), PasswordHash(passwordHash))
}
