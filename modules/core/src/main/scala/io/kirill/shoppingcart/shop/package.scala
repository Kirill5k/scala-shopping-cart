package io.kirill.shoppingcart

import cats.effect.{Resource, Sync}
import cats.implicits._
import dev.profunktor.redis4cats.algebra.RedisCommands
import io.chrisdavenport.log4cats.Logger
import io.kirill.shoppingcart.auth.Auth
import io.kirill.shoppingcart.config.AppConfig
import io.kirill.shoppingcart.shop.brand.{BrandRepository, BrandService}
import io.kirill.shoppingcart.shop.cart.CartService
import io.kirill.shoppingcart.shop.category.{CategoryRepository, CategoryService}
import io.kirill.shoppingcart.shop.item.{ItemRepository, ItemService}
import io.kirill.shoppingcart.shop.order.{OrderRepository, OrderService}
import io.kirill.shoppingcart.shop.payment.PaymentService

package object shop {

  final class Shop[F[_]](
      )

  object Shop {
    def make[F[_]: Sync: Logger](
        res: Resources[F]
    )(implicit config: AppConfig): F[Shop[F]] =
      for {
        brandService    <- BrandRepository.make(res.postgres).flatMap(BrandService.make)
        cartService     <- CartService.redisCartService(res.redis)
        categoryService <- CategoryRepository.make(res.postgres).flatMap(CategoryService.make)
        itemService     <- ItemRepository.make(res.postgres).flatMap(ItemService.make)
        paymentService  <- PaymentService.make[F]()
        orderService    <- OrderRepository.make(res.postgres).flatMap(OrderService.make)
      } yield new Shop[F]()
  }
}
