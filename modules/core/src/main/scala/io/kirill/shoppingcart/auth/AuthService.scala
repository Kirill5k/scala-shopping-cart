package io.kirill.shoppingcart.auth

import cats.implicits._
import cats.{Monad, MonadError}
import dev.profunktor.auth.jwt.JwtToken
import io.kirill.shoppingcart.auth.user._
import io.kirill.shoppingcart.common.errors.{InvalidUsernameOrPassword, UniqueViolation, UsernameInUse}

trait AuthService[F[_]] {
  def login(username: Username, password: Password): F[JwtToken]
  def logout(token: JwtToken, username: Username): F[Unit]
  def create(username: Username, password: Password): F[UserId]
}

final private class LiveAuthService[F[_]: MonadError[*[_], Throwable]](
    userRepository: UserRepository[F],
    userCacheStore: UserCacheStore[F],
    tokenGenerator: TokenGenerator[F],
    passwordEncryptor: PasswordEncryptor[F]
) extends AuthService[F] {

  override def login(username: Username, password: Password): F[JwtToken] =
    userRepository.findByName(username).flatMap {
      case None => InvalidUsernameOrPassword(username).raiseError[F, JwtToken]
      case Some(u) =>
        u.password
          .fold(false.pure[F])(ph => passwordEncryptor.isValid(password, ph))
          .flatMap {
            case false => InvalidUsernameOrPassword(username).raiseError[F, JwtToken]
            case true =>
              userCacheStore.findToken(username).flatMap {
                case Some(t) => t.pure[F]
                case None    => tokenGenerator.generate.flatMap(t => userCacheStore.put(t, u) *> t.pure[F])
              }
          }
    }

  override def logout(token: JwtToken, username: Username): F[Unit] =
    userCacheStore.remove(token, username)

  override def create(username: Username, password: Password): F[UserId] =
    (for {
      hash <- passwordEncryptor.hash(password)
      uid  <- userRepository.create(username, hash)
    } yield uid).handleErrorWith { case UniqueViolation(_) =>
      UsernameInUse(username).raiseError[F, UserId]
    }
}

object AuthService {
  def make[F[_]: MonadError[*[_], Throwable]](
      userRepository: UserRepository[F],
      userCacheStore: UserCacheStore[F],
      tokenGenerator: TokenGenerator[F],
      passwordEncryptor: PasswordEncryptor[F]
  ): F[AuthService[F]] =
    Monad[F].pure(new LiveAuthService[F](userRepository, userCacheStore, tokenGenerator, passwordEncryptor))
}
