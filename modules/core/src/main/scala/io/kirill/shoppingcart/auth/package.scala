package io.kirill.shoppingcart

import java.util.UUID

import cats.effect.{Resource, Sync}
import cats.implicits._
import dev.profunktor.auth.jwt.{JwtAuth, JwtSymmetricAuth, JwtToken}
import dev.profunktor.redis4cats.algebra.RedisCommands
import io.chrisdavenport.log4cats.Logger
import io.kirill.shoppingcart.auth.user.{User, UserCacheStore, UserRepository}
import io.kirill.shoppingcart.config.AppConfig
import pdi.jwt.JwtAlgorithm
import skunk.Session

package object auth {
  final case class AdminClaimContent(id: UUID)           extends AnyVal
  final case class AdminJwtAuth(value: JwtSymmetricAuth) extends AnyVal
  final case class UserJwtAuth(value: JwtSymmetricAuth)  extends AnyVal

  final case class CommonUser(value: User) extends AnyVal
  final case class AdminUser(value: User)  extends AnyVal

  final class Auth[F[_]](
      val authService: AuthService[F],
      val adminAuth: Authenticator[F, AdminUser],
      val userAuth: Authenticator[F, CommonUser],
      val adminJwtAuth: AdminJwtAuth,
      val userJwtAuth: UserJwtAuth,
      val authController: AuthController[F]
  )

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
      } yield new Auth[F](authService, adminAuth, userAuth, adminJwtAuth, userJwtAuth, authController)
    }
  }
}
