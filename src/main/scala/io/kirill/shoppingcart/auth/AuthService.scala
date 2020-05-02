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
  def create(username: Username, password: Password): F[JwtToken]
}

final class LiveAuthService[F[_]: Sync] private (
    userRepository: UserRepository[F],
    userCacheStore: UserCacheStore[F],
    tokenGenerator: TokenGenerator[F],
    passwordEncryptor: PasswordEncryptor[F]
)(
    implicit val appConfig: AppConfig
) extends AuthService[F] {

  override def login(username: Username, password: Password): F[JwtToken] =
    userRepository.findByName(username).flatMap {
      case None => InvalidUsernameOrPassword.raiseError[F, JwtToken]
      case Some(u) =>
        for {
          isValidPassword <- passwordEncryptor.isValid(password, u.password)
          token = if (isValidPassword) userCacheStore.findToken(username).flatMap {
            case Some(t) => t.pure[F]
            case None    => tokenGenerator.generate.flatMap(t => userCacheStore.put(t, u) *> t.pure[F])
          } else InvalidUsernameOrPassword.raiseError[F, JwtToken]
        } yield token
    }

  override def logout(token: JwtToken, username: Username): F[Unit] =
    userCacheStore.remove(token, username)

  override def create(username: Username, password: Password): F[JwtToken] =
    (for {
      hash  <- passwordEncryptor.hash(password)
      uid   <- userRepository.create(username, hash)
      token <- tokenGenerator.generate
      _     <- userCacheStore.put(token, User(uid, username, hash))
    } yield token).handleErrorWith {
      case UniqueViolation(_) => UsernameInUse(username).raiseError[F, JwtToken]
    }
}
