package io.kirill.shoppingcart.auth

import cats.implicits._
import cats.{Monad, MonadError}
import dev.profunktor.auth.jwt.JwtToken
import io.kirill.shoppingcart.auth.user._
import io.kirill.shoppingcart.auth.utils.{PasswordEncryptor, TokenGenerator}
import io.kirill.shoppingcart.common.errors.{InvalidUsernameOrPassword, UniqueViolation, UsernameInUse}

trait AuthService[F[_]] {
  def login(username: User.Name, password: User.Password): F[JwtToken]
  def logout(token: JwtToken, username: User.Name): F[Unit]
  def create(username: User.Name, password: User.Password): F[User.Id]
}

final private class LiveAuthService[F[_]: MonadError[*[_], Throwable]](
    userRepository: UserRepository[F],
    userCacheStore: UserCacheStore[F],
    tokenGenerator: TokenGenerator[F],
    passwordEncryptor: PasswordEncryptor[F]
) extends AuthService[F] {

  override def login(username: User.Name, password: User.Password): F[JwtToken] =
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

  override def logout(token: JwtToken, username: User.Name): F[Unit] =
    userCacheStore.remove(token, username)

  override def create(username: User.Name, password: User.Password): F[User.Id] =
    (for {
      hash <- passwordEncryptor.hash(password)
      uid  <- userRepository.create(username, hash)
    } yield uid).handleErrorWith { case UniqueViolation(_) =>
      UsernameInUse(username).raiseError[F, User.Id]
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
