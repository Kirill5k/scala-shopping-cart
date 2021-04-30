package io.kirill.shoppingcart.auth.user

import cats.Monad
import cats.implicits._
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import io.circe.generic.auto._
import io.kirill.shoppingcart.common.json._
import io.circe.parser.decode
import io.circe.syntax._
import io.kirill.shoppingcart.config.AuthConfig

import scala.concurrent.duration.FiniteDuration

trait UserCacheStore[F[_]] {
  def findUser(token: JwtToken): F[Option[User]]
  def findToken(username: User.Name): F[Option[JwtToken]]
  def put(token: JwtToken, user: User): F[Unit]
  def remove(token: JwtToken, username: User.Name): F[Unit]
}

final private class RedisUserCacheStore[F[_]: Monad](
    redis: RedisCommands[F, String, String],
    tokenExpiration: FiniteDuration
) extends UserCacheStore[F] {

  override def findUser(token: JwtToken): F[Option[User]] =
    redis
      .get(token.value)
      .map(_.flatMap { json =>
        decode[User](json).toOption
      })

  override def put(token: JwtToken, user: User): F[Unit] =
    redis.setEx(token.value, user.asJson.noSpaces, tokenExpiration) *>
      redis.setEx(user.name.value, token.value, tokenExpiration)

  override def findToken(username: User.Name): F[Option[JwtToken]] =
    redis.get(username.value).map(_.map(JwtToken))

  override def remove(token: JwtToken, username: User.Name): F[Unit] =
    redis.del(token.value) *> redis.del(username.value).void
}

object UserCacheStore {
  def redisUserCacheStore[F[_]: Monad](
      redis: RedisCommands[F, String, String],
      config: AuthConfig
  ): F[UserCacheStore[F]] =
    Monad[F].pure(new RedisUserCacheStore[F](redis, config.userJwt.tokenExpiration))
}
