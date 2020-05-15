package io.kirill.shoppingcart.auth

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt.JwtToken
import io.kirill.shoppingcart.auth.user.{Password, User, UserCacheStore, UserId, UserRepository, Username}
import io.kirill.shoppingcart.common.errors.{InvalidUsernameOrPassword, UniqueViolation, UsernameInUse}
import io.kirill.shoppingcart.config.AppConfig

trait AuthService[F[_]] {
  def login(username: Username, password: Password): F[JwtToken]
  def logout(token: JwtToken, username: Username): F[Unit]
  def create(username: Username, password: Password): F[UserId]
}

final private class LiveAuthService[F[_]: Sync](
    userRepository: UserRepository[F],
    userCacheStore: UserCacheStore[F],
    tokenGenerator: TokenGenerator[F],
    passwordEncryptor: PasswordEncryptor[F]
)(
    implicit val appConfig: AppConfig
) extends AuthService[F] {

  override def login(username: Username, password: Password): F[JwtToken] =
    userRepository.findByName(username).flatMap {
      case None => InvalidUsernameOrPassword(username).raiseError[F, JwtToken]
      case Some(u) =>
        for {
          isValidPassword <- u.password.fold(false.pure[F])(ph => passwordEncryptor.isValid(password, ph))
          token <- if (isValidPassword) userCacheStore.findToken(username).flatMap {
            case Some(t) => t.pure[F]
            case None    => tokenGenerator.generate.flatMap(t => userCacheStore.put(t, u) *> t.pure[F])
          } else InvalidUsernameOrPassword(username).raiseError[F, JwtToken]
        } yield token
    }

  override def logout(token: JwtToken, username: Username): F[Unit] =
    userCacheStore.remove(token, username)

  override def create(username: Username, password: Password): F[UserId] =
    (for {
      hash <- passwordEncryptor.hash(password)
      uid  <- userRepository.create(username, hash)
    } yield uid).handleErrorWith {
      case UniqueViolation(_) => UsernameInUse(username).raiseError[F, UserId]
    }
}

object AuthService {
  def make[F[_]: Sync](
      userRepository: UserRepository[F],
      userCacheStore: UserCacheStore[F],
      tokenGenerator: TokenGenerator[F],
      passwordEncryptor: PasswordEncryptor[F]
  )(
      implicit config: AppConfig
  ): F[AuthService[F]] =
    Sync[F].delay(new LiveAuthService[F](userRepository, userCacheStore, tokenGenerator, passwordEncryptor))
}
