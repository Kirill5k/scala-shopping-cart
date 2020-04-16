package io.kirill.shoppingcart.common.security

import io.kirill.shoppingcart.common.security.user.{Password, User, UserId, UserName}

trait UserService[F[_]] {
  def findBy(name: UserName): F[Option[User]]
  def create(name: UserName, password: Password): F[UserId]
}
