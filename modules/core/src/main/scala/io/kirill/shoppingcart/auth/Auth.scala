package io.kirill.shoppingcart.auth

import cats.effect.{Resource, Sync}
import cats.implicits._
import dev.profunktor.auth.jwt._
import dev.profunktor.redis4cats.algebra.RedisCommands
import io.chrisdavenport.log4cats.Logger
import io.kirill.shoppingcart.auth.user.{UserCacheStore, UserRepository}
import io.kirill.shoppingcart.config.AppConfig
import pdi.jwt._
import skunk.Session

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
      session: Resource[F, Session[F]],
      redis: RedisCommands[F, String, String]
  )(implicit config: AppConfig): F[Auth[F]] = {

    val adminJwtAuth: AdminJwtAuth =
      AdminJwtAuth(JwtAuth.hmac(config.auth.adminJwt.secretKey, JwtAlgorithm.HS256))

    val userJwtAuth: UserJwtAuth =
      UserJwtAuth(JwtAuth.hmac(config.auth.userJwt.secretKey, JwtAlgorithm.HS256))

    for {
      repo        <- UserRepository.make[F](session)
      caheStore   <- UserCacheStore.redisUserCacheStore[F](redis)
      tokenGen    <- TokenGenerator.make[F]
      encryptor   <- PasswordEncryptor.make[F]
      authService <- AuthService.make(repo, caheStore, tokenGen, encryptor)
      userAuth    <- Authenticator.commonUserAuthenticator(caheStore)
      adminToken = JwtToken(config.auth.adminJwt.token)
      adminAuth <- Authenticator.adminUserAuthenticator(adminToken, adminJwtAuth)
      authController <- AuthController.make(authService)
    } yield new Auth[F](authService, adminAuth, userAuth, adminJwtAuth, userJwtAuth, authController)
  }
}
