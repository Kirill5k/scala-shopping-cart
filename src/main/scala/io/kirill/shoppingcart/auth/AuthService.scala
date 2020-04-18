package io.kirill.shoppingcart.auth

trait AuthService[F[_]] {
  def login(userName: Username, password: Password): F[JwtToken]
  def logout(userId: UserId, token: JwtToken): F[Unit]
  def create(userName: Username, password: Password): F[UserId]
}
