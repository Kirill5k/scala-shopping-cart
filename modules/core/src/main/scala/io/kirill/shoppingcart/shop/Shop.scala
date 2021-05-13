package io.kirill.shoppingcart.shop

import cats.effect.Sync
import cats.implicits._
import io.kirill.shoppingcart.Resources
import io.kirill.shoppingcart.auth.{AdminUser, CommonUser}
import io.kirill.shoppingcart.config.ShopConfig
import io.kirill.shoppingcart.shop.brand.{BrandController, BrandRepository, BrandService}
import io.kirill.shoppingcart.shop.cart.{CartController, CartService}
import io.kirill.shoppingcart.shop.category.{CategoryController, CategoryRepository, CategoryService}
import io.kirill.shoppingcart.shop.item.{ItemController, ItemRepository, ItemService}
import io.kirill.shoppingcart.shop.order.{OrderController, OrderRepository, OrderService}
import io.kirill.shoppingcart.shop.payment.{PaymentClient, PaymentService}
import org.http4s.HttpRoutes
import org.http4s.server.{AuthMiddleware, Router}
import org.typelevel.log4cats.Logger

final class Shop[F[_]: Sync] private (
    val brandController: BrandController[F],
    val categoryController: CategoryController[F],
    val cartController: CartController[F],
    val itemController: ItemController[F],
    val orderController: OrderController[F]
) {

  def routes(
      userAuthMiddleware: AuthMiddleware[F, CommonUser],
      adminAuthMiddleware: AuthMiddleware[F, AdminUser]
  ): HttpRoutes[F] = {
    val allRoutes =
      brandController.routes(adminAuthMiddleware) <+>
        categoryController.routes(adminAuthMiddleware) <+>
        cartController.routes(userAuthMiddleware) <+>
        itemController.routes(adminAuthMiddleware) <+>
        orderController.routes(userAuthMiddleware)
    Router("/shop" -> allRoutes)
  }
}

object Shop {
  def make[F[_]: Sync: Logger](
      res: Resources[F],
      config: ShopConfig
  ): F[Shop[F]] =
    for {
      brandService       <- BrandRepository.make(res.postgres).flatMap(BrandService.make[F])
      brandController    <- BrandController.make(brandService)
      cartService        <- CartService.redisCartService(res.redis, config)
      cartController     <- CartController.make(cartService)
      categoryService    <- CategoryRepository.make(res.postgres).flatMap(CategoryService.make[F])
      categoryController <- CategoryController.make(categoryService)
      itemService        <- ItemRepository.make(res.postgres).flatMap(ItemService.make[F])
      itemController     <- ItemController.make(itemService)
      paymentClient      <- PaymentClient.make[F](config.payment)
      paymentService     <- PaymentService.make[F](paymentClient)
      orderService       <- OrderRepository.make(res.postgres).flatMap(OrderService.make[F])
      orderController    <- OrderController.make(orderService, cartService, itemService, paymentService)
    } yield new Shop[F](
      brandController,
      categoryController,
      cartController,
      itemController,
      orderController
    )
}
