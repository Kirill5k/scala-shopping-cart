package io.kirill.shoppingcart.auth.user

import cats.effect.{Resource, Sync}
import cats.implicits._
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.algebra.RedisCommands
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import io.kirill.shoppingcart.common.json._
import io.kirill.shoppingcart.config.AppConfig

sealed trait UserCacheStore[F[_]] {
  def findUser(token: JwtToken): F[Option[User]]
  def findToken(username: Username): F[Option[JwtToken]]
  def put(token: JwtToken, user: User): F[Unit]
  def remove(token: JwtToken, username: Username): F[Unit]
}

final private class RedisUserCacheStore[F[_]: Sync](
    redis: RedisCommands[F, String, String]
)(
    implicit config: AppConfig
) extends UserCacheStore[F] {

  override def findUser(token: JwtToken): F[Option[User]] =
    redis
      .get(token.value)
      .map(_.flatMap { json =>
        decode[User](json).toOption
      })

  override def put(token: JwtToken, user: User): F[Unit] =
    redis.setEx(token.value, user.asJson.noSpaces, config.auth.userJwt.tokenExpiration) *>
      redis.setEx(user.name.value, token.value, config.auth.userJwt.tokenExpiration)

  override def findToken(username: Username): F[Option[JwtToken]] =
    redis.get(username.value).map(_.map(JwtToken))

  override def remove(token: JwtToken, username: Username): F[Unit] =
    redis.del(token.value) *> redis.del(username.value)
}

object UserCacheStore {
  def redisUserCacheStore[F[_]: Sync](
      redis: RedisCommands[F, String, String]
  )(
      implicit config: AppConfig
  ): F[UserCacheStore[F]] =
    Sync[F].delay(new RedisUserCacheStore[F](redis))
}
