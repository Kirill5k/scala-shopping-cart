package io.kirill.shoppingcart.auth

import java.util.UUID

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt._
import io.circe.syntax._
import io.kirill.shoppingcart.config.SecurityConfig
import pdi.jwt._

trait TokenGenerator[F[_]] {
  def generate: F[JwtToken]
}

object TokenGenerator {

  private def hs256TokenGenerator[F[_]: Sync](config: SecurityConfig)(implicit ev: java.time.Clock): TokenGenerator[F] =
    new TokenGenerator[F] {
      override def generate: F[JwtToken] =
        for {
          id <- Sync[F].delay(UUID.randomUUID().asJson.noSpaces)
          claim = JwtClaim(id).issuedNow.expiresIn(config.tokenExpiration.toMillis)
          key   = JwtSecretKey(config.jwtSecretKey)
          jwt <- jwtEncode[F](claim, key, JwtAlgorithm.HS256)
        } yield jwt
    }

  def apply[F[_]: Sync](config: SecurityConfig)(implicit ev: java.time.Clock): TokenGenerator[F] =
    hs256TokenGenerator(config)
}
