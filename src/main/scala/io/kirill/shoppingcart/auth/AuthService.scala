package io.kirill.shoppingcart.auth

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.algebra.RedisCommands
import io.circe.parser._
import io.kirill.shoppingcart.auth.user.{Password, User, UserId, Username}
import io.kirill.shoppingcart.config.AppConfig
import pdi.jwt.JwtClaim

trait AuthService[F[_]] {
  def login(username: Username, password: Password): F[JwtToken]
  def logout(username: Username, token: JwtToken): F[Unit]
  def create(username: Username, password: Password): F[UserId]
}
