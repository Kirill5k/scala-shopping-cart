package io.kirill.shoppingcart.auth.user

import cats.effect.{Resource, Sync}
import cats.implicits._
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.algebra.RedisCommands
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.generic.extras.semiauto._
import io.circe.parser.decode
import io.kirill.shoppingcart.auth.{AdminUser, CommonUser}
import pdi.jwt.JwtClaim

sealed trait UserStore[F[_], A] {
  def findUser(token: JwtToken)(claim: JwtClaim): F[Option[A]]
}

final private class CommonUserStore[F[_]: Sync](
    redis: Resource[F, RedisCommands[F, String, String]]
) extends UserStore[F, CommonUser] {

  implicit val uidDecoder: Decoder[UserId] = deriveUnwrappedDecoder
  implicit val unameDecoder: Decoder[Username] = deriveUnwrappedDecoder
  implicit val passwordHashDecoder: Decoder[PasswordHash] = deriveUnwrappedDecoder

  override def findUser(token: JwtToken)(claim: JwtClaim): F[Option[CommonUser]] =
    redis.use { r =>
      r.get(token.value)
        .map(_.flatMap { json =>
          decode[User](json).toOption.map(CommonUser.apply)
        })
    }
}

final private class AdminUserStore[F[_]: Sync](
    adminToken: JwtToken,
    adminUser: AdminUser
) extends UserStore[F, AdminUser] {

  override def findUser(token: JwtToken)(claim: JwtClaim): F[Option[AdminUser]] =
    (token == adminToken).guard[Option].as(adminUser).pure[F]
}

object UserStore {
  def commonUserStore[F[_]: Sync](redis: Resource[F, RedisCommands[F, String, String]]): F[UserStore[F, CommonUser]] =
    Sync[F].delay(new CommonUserStore[F](redis))

  def adminUserStore[F[_]: Sync](adminToken: JwtToken, adminUser: AdminUser): F[UserStore[F, AdminUser]] =
    Sync[F].delay(new AdminUserStore[F](adminToken, adminUser))
}
