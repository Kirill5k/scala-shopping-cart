package io.kirill.shoppingcart.common.auth

trait UserService[F[_]] {
  def findBy(name: UserName): F[Option[User]]
  def create(name: UserName, password: Password): F[UserId]
}
