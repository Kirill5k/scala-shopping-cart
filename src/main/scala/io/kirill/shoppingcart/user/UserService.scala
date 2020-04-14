package io.kirill.shoppingcart.user

trait UserService[F[_]] {
  def findBy(name: UserName): F[Option[User]]
  def create(name: UserName, password: Password): F[UserId]
}
