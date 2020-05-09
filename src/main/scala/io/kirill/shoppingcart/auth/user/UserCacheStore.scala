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
    redis: Resource[F, RedisCommands[F, String, String]]
)(
    implicit config: AppConfig
) extends UserCacheStore[F] {

  override def findUser(token: JwtToken): F[Option[User]] =
    redis.use { r =>
      r.get(token.value)
        .map(_.flatMap { json =>
          decode[User](json).toOption
        })
    }

  override def put(token: JwtToken, user: User): F[Unit] =
    redis.use { r =>
      r.setEx(token.value, user.asJson.noSpaces, config.auth.tokenExpiration) *>
        r.setEx(user.name.value, token.value, config.auth.tokenExpiration)
    }

  override def findToken(username: Username): F[Option[JwtToken]] =
    redis.use { r =>
      r.get(username.value).map(_.map(JwtToken))
    }

  override def remove(token: JwtToken, username: Username): F[Unit] =
    redis.use { r =>
      r.del(token.value) *> r.del(username.value)
    }
}

object UserStore {
  def redisUserCacheStore[F[_]: Sync](
      redis: Resource[F, RedisCommands[F, String, String]]
  )(
      implicit config: AppConfig
  ): F[UserCacheStore[F]] =
    Sync[F].delay(new RedisUserCacheStore[F](redis))
}
