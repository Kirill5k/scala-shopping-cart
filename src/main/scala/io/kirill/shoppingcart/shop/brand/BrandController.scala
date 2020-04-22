package io.kirill.shoppingcart.shop.brand

import cats.effect.Sync
import io.circe.generic.auto._
import io.kirill.shoppingcart.common.web.RestController
import org.http4s.circe.CirceEntityCodec._
import org.http4s.HttpRoutes
import org.http4s.server.Router

final class BrandController[F[_]: Sync](brandService: BrandService[F]) extends RestController[F] {
  private val prefixPath = "/brands"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => withErrorHandling {
      Ok(brandService.findAll)
    }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
