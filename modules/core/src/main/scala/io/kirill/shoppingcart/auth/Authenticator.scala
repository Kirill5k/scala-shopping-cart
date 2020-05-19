package io.kirill.shoppingcart.auth

import java.util.UUID

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt._
import io.circe.parser.{decode => jsonDecode}
import io.circe.generic.auto._
import io.kirill.shoppingcart.auth.user.{User, UserCacheStore, UserId, Username}
import pdi.jwt.JwtClaim

sealed trait Authenticator[F[_], U] {
  def findUser(token: JwtToken)(claim: JwtClaim): F[Option[U]]
}

final private class CommonUserAuthenticator[F[_]: Sync](
    userCacheStore: UserCacheStore[F]
) extends Authenticator[F, CommonUser] {
  override def findUser(token: JwtToken)(claim: JwtClaim): F[Option[CommonUser]] =
    userCacheStore.findUser(token).map(_.map(CommonUser))
}

final private class AdminUserAuthenticator[F[_]: Sync](
    adminToken: JwtToken,
    adminUser: AdminUser
) extends Authenticator[F, AdminUser] {
  override def findUser(token: JwtToken)(claim: JwtClaim): F[Option[AdminUser]] =
    (token == adminToken).guard[Option].as(adminUser).pure[F]
}

object Authenticator {
  def commonUserAuthenticator[F[_]: Sync](userCacheStore: UserCacheStore[F]): F[Authenticator[F, CommonUser]] =
    Sync[F].delay(new CommonUserAuthenticator[F](userCacheStore))

  def adminUserAuthenticator[F[_]: Sync](adminToken: JwtToken, adminJwtAuth: AdminJwtAuth): F[Authenticator[F, AdminUser]] =
    for {
      claim      <- jwtDecode[F](adminToken, adminJwtAuth.value)
      adminClaim <- Sync[F].fromEither(jsonDecode[AdminClaimContent](claim.content))
      adminUser = AdminUser(User(UserId(adminClaim.id), Username("admin"), None))
    } yield new AdminUserAuthenticator[F](adminToken, adminUser)
}
