package io.kirill.shoppingcart

import cats.effect.{Resource, Sync}
import cats.implicits._
import dev.profunktor.redis4cats.algebra.RedisCommands
import io.chrisdavenport.log4cats.Logger
import io.kirill.shoppingcart.auth.Auth
import io.kirill.shoppingcart.config.AppConfig
import io.kirill.shoppingcart.shop.brand.{BrandController, BrandRepository, BrandService}
import io.kirill.shoppingcart.shop.cart.{CartController, CartService}
import io.kirill.shoppingcart.shop.category.{CategoryController, CategoryRepository, CategoryService}
import io.kirill.shoppingcart.shop.item.{ItemController, ItemRepository, ItemService}
import io.kirill.shoppingcart.shop.order.{OrderController, OrderRepository, OrderService}
import io.kirill.shoppingcart.shop.payment.PaymentService

package object shop {

  final class Shop[F[_]](
      val brandController: BrandController[F],
      val categoryController: CategoryController[F],
      val cartController: CartController[F],
      val itemController: ItemController[F],
      val orderController: OrderController[F]
  )

  object Shop {
    def make[F[_]: Sync: Logger](
        res: Resources[F]
    )(implicit config: AppConfig): F[Shop[F]] =
      for {
        brandService       <- BrandRepository.make(res.postgres).flatMap(BrandService.make)
        brandController    <- BrandController.make(brandService)
        cartService        <- CartService.redisCartService(res.redis)
        cartController     <- CartController.make(cartService)
        categoryService    <- CategoryRepository.make(res.postgres).flatMap(CategoryService.make)
        categoryController <- CategoryController.make(categoryService)
        itemService        <- ItemRepository.make(res.postgres).flatMap(ItemService.make)
        itemController     <- ItemController.make(itemService)
        paymentService     <- PaymentService.make[F]()
        orderService       <- OrderRepository.make(res.postgres).flatMap(OrderService.make)
        orderController    <- OrderController.make(orderService, cartService, itemService, paymentService)
      } yield new Shop[F](
        brandController,
        categoryController,
        cartController,
        itemController,
        orderController
      )
  }
}
