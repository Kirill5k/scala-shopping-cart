package io.kirill.shoppingcart.auth

import dev.profunktor.auth.jwt.JwtToken
import io.kirill.shoppingcart.auth.user.{Password, UserId, Username}

trait AuthService[F[_]] {
  def login(username: Username, password: Password): F[JwtToken]
  def logout(username: Username, token: JwtToken): F[Unit]
  def create(username: Username, password: Password): F[UserId]
}
