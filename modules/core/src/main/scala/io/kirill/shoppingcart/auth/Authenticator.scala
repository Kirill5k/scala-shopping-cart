package io.kirill.shoppingcart.auth

import java.util.UUID

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt._
import io.circe.parser.{decode => jsonDecode}
import io.kirill.shoppingcart.auth.user.{User, UserCacheStore, UserId, Username}
import io.kirill.shoppingcart.config.AppConfig
import pdi.jwt.{JwtAlgorithm, JwtClaim}

sealed trait Authenticator[F[_], U] {
  def findUser(token: JwtToken)(claim: JwtClaim): F[Option[U]]
}

final class CommonUserAuthenticator[F[_]: Sync](
    userCacheStore: UserCacheStore[F]
) extends Authenticator[F, CommonUser] {
  override def findUser(token: JwtToken)(claim: JwtClaim): F[Option[CommonUser]] =
    userCacheStore.findUser(token).map(_.map(CommonUser))
}

final class AdminUserAuthenticator[F[_]: Sync](
    adminToken: JwtToken,
    adminUser: AdminUser
) extends Authenticator[F, AdminUser] {
  override def findUser(token: JwtToken)(claim: JwtClaim): F[Option[AdminUser]] =
    (token == adminToken).guard[Option].as(adminUser).pure[F]
}

object Authenticator {
  def commonUserAuthenticator[F[_]: Sync](userCacheStore: UserCacheStore[F]): F[Authenticator[F, CommonUser]] =
    Sync[F].delay(new CommonUserAuthenticator[F](userCacheStore))

  def adminUserAuthenticator[F[_]: Sync](adminJwtAuth: AdminJwtAuth)(implicit config: AppConfig): F[Authenticator[F, AdminUser]] =
    for {
      adminToken <- Sync[F].delay(JwtToken(config.auth.adminJwt.token))
      claim <- jwtDecode[F](adminToken, adminJwtAuth.value)
      adminClaim <- Sync[F].fromEither(jsonDecode[UUID](claim.content))
      adminUser = AdminUser(User(UserId(adminClaim), Username("admin"), None))
    } yield new AdminUserAuthenticator[F](adminToken, adminUser)
}
