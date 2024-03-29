package io.kirill.shoppingcart.auth

import cats.Monad
import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt._
import io.circe.parser.{decode => jsonDecode}
import io.circe.generic.auto._
import io.kirill.shoppingcart.auth.user.{User, UserCacheStore}
import pdi.jwt.JwtClaim

trait Authenticator[F[_], U] {
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
    Monad[F].pure(new CommonUserAuthenticator[F](userCacheStore))

  def adminUserAuthenticator[F[_]: Sync](adminToken: JwtToken, adminJwtAuth: AdminJwtAuth): F[Authenticator[F, AdminUser]] =
    for {
      claim      <- jwtDecode[F](adminToken, adminJwtAuth.value)
      adminClaim <- Sync[F].fromEither(jsonDecode[AdminClaimContent](claim.content))
      adminUser = AdminUser(User(User.Id(adminClaim.id), User.Name("admin"), None))
    } yield new AdminUserAuthenticator[F](adminToken, adminUser)
}
