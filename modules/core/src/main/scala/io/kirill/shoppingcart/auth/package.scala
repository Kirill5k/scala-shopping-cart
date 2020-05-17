package io.kirill.shoppingcart

import java.util.UUID

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.JwtAuthMiddleware
import dev.profunktor.auth.jwt.{JwtAuth, JwtSymmetricAuth, JwtToken}
import io.chrisdavenport.log4cats.Logger
import io.kirill.shoppingcart.auth.user.{User, UserCacheStore, UserRepository}
import io.kirill.shoppingcart.config.AppConfig
import pdi.jwt.JwtAlgorithm

package object auth {
  final case class AdminClaimContent(id: UUID)           extends AnyVal
  final case class AdminJwtAuth(value: JwtSymmetricAuth) extends AnyVal
  final case class UserJwtAuth(value: JwtSymmetricAuth)  extends AnyVal

  final case class CommonUser(value: User) extends AnyVal
  final case class AdminUser(value: User)  extends AnyVal

  final class Auth[F[_]: Sync](
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
        res: Resources[F]
    )(implicit config: AppConfig): F[Auth[F]] = {

      val adminJwtAuth: AdminJwtAuth =
        AdminJwtAuth(JwtAuth.hmac(config.auth.adminJwt.secretKey, JwtAlgorithm.HS256))

      val userJwtAuth: UserJwtAuth =
        UserJwtAuth(JwtAuth.hmac(config.auth.userJwt.secretKey, JwtAlgorithm.HS256))

      for {
        repo        <- UserRepository.make[F](res.postgres)
        caheStore   <- UserCacheStore.redisUserCacheStore[F](res.redis)
        tokenGen    <- TokenGenerator.make[F]
        encryptor   <- PasswordEncryptor.make[F]
        authService <- AuthService.make(repo, caheStore, tokenGen, encryptor)
        userAuth    <- Authenticator.commonUserAuthenticator(caheStore)
        adminToken = JwtToken(config.auth.adminJwt.token)
        adminAuth      <- Authenticator.adminUserAuthenticator(adminToken, adminJwtAuth)
        authController <- AuthController.make(authService)
      } yield new Auth[F](adminJwtAuth, adminAuth, userJwtAuth, userAuth, authController)
    }
  }
}
