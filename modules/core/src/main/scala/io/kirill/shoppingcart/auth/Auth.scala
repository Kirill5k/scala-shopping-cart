package io.kirill.shoppingcart.auth

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.JwtAuthMiddleware
import dev.profunktor.auth.jwt.{JwtAuth, JwtSymmetricAuth, JwtToken}
import io.kirill.shoppingcart.Resources
import io.kirill.shoppingcart.auth.user.{User, UserCacheStore, UserRepository}
import io.kirill.shoppingcart.config.AuthConfig
import org.typelevel.log4cats.Logger
import pdi.jwt.JwtAlgorithm

import java.util.UUID

final case class AdminClaimContent(id: UUID)           extends AnyVal
final case class AdminJwtAuth(value: JwtSymmetricAuth) extends AnyVal
final case class UserJwtAuth(value: JwtSymmetricAuth)  extends AnyVal

final case class CommonUser(value: User) extends AnyVal
final case class AdminUser(value: User)  extends AnyVal

final class Auth[F[_]: Sync] private (
    private val adminJwtAuth: AdminJwtAuth,
    private val adminAuth: Authenticator[F, AdminUser],
    private val userJwtAuth: UserJwtAuth,
    private val userAuth: Authenticator[F, CommonUser],
    val authController: AuthController[F]
) {
  val adminMiddleware = JwtAuthMiddleware[F, AdminUser](adminJwtAuth.value, adminAuth.findUser)
  val userMiddleware  = JwtAuthMiddleware[F, CommonUser](userJwtAuth.value, userAuth.findUser)
}

object Auth {
  def make[F[_]: Sync: Logger](
      res: Resources[F],
      config: AuthConfig
  ): F[Auth[F]] = {

    val adminJwtAuth: AdminJwtAuth =
      AdminJwtAuth(JwtAuth.hmac(config.adminJwt.secretKey, JwtAlgorithm.HS256))

    val userJwtAuth: UserJwtAuth =
      UserJwtAuth(JwtAuth.hmac(config.userJwt.secretKey, JwtAlgorithm.HS256))

    for {
      repo        <- UserRepository.make[F](res.postgres)
      caheStore   <- UserCacheStore.redisUserCacheStore[F](res.redis, config)
      tokenGen    <- TokenGenerator.make[F](config)
      encryptor   <- PasswordEncryptor.make[F](config)
      authService <- AuthService.make[F](repo, caheStore, tokenGen, encryptor)
      userAuth    <- Authenticator.commonUserAuthenticator(caheStore)
      adminToken = JwtToken(config.adminJwt.token)
      adminAuth      <- Authenticator.adminUserAuthenticator(adminToken, adminJwtAuth)
      authController <- AuthController.make(authService)
    } yield new Auth[F](adminJwtAuth, adminAuth, userJwtAuth, userAuth, authController)
  }
}
