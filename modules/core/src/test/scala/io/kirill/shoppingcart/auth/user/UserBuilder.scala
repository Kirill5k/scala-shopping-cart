package io.kirill.shoppingcart.auth.user

import java.util.UUID

object UserBuilder {

  def user(name: String = "Boris", passwordHash: String = "password-hash"): User =
    User(User.Id(UUID.randomUUID()), User.Name(name), Some(User.PasswordHash(passwordHash)))
}
