package io.kirill.shoppingcart.auth

import java.util.UUID

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt._
import io.circe.syntax._
import io.kirill.shoppingcart.config.{AppConfig, AuthConfig}
import pdi.jwt._

trait TokenGenerator[F[_]] {
  def generate: F[JwtToken]
}

object TokenGenerator {

  private def hs256TokenGenerator[F[_]: Sync](implicit config: AppConfig, ev: java.time.Clock): TokenGenerator[F] =
    new TokenGenerator[F] {
      override def generate: F[JwtToken] =
        for {
          id <- Sync[F].delay(UUID.randomUUID().asJson.noSpaces)
          claim = JwtClaim(id).issuedNow.expiresIn(config.auth.tokenExpiration.toMillis)
          key   = JwtSecretKey(config.auth.jwtSecretKey)
          jwt <- jwtEncode[F](claim, key, JwtAlgorithm.HS256)
        } yield jwt
    }

  def apply[F[_]: Sync](implicit config: AuthConfig, ev: java.time.Clock): TokenGenerator[F] =
    hs256TokenGenerator
}
