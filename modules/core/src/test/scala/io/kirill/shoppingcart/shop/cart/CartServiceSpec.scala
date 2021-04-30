package io.kirill.shoppingcart.shop.cart

import java.util.UUID
import io.kirill.shoppingcart.RedisSpec
import io.kirill.shoppingcart.auth.user.User
import io.kirill.shoppingcart.config.ShopConfig
import io.kirill.shoppingcart.shop.item.Item

import scala.concurrent.duration._

class CartServiceSpec extends RedisSpec {

  val shopConfig = ShopConfig(1.day)

  val userId  = User.Id(UUID.randomUUID())
  val itemId1 = Item.Id(UUID.randomUUID())
  val itemId2 = Item.Id(UUID.randomUUID())

  "A RedisCartService" - {

    "add" - {
      "add items to a cart" in {
        withRedisCommands { redis =>
          val result = for {
            service <- CartService.redisCartService(redis, shopConfig)
            _       <- service.add(userId, Cart(List(CartItem(itemId1, Item.Quantity(4)))))
            _       <- service.add(userId, Cart(List(CartItem(itemId1, Item.Quantity(4)), CartItem(itemId2, Item.Quantity(4)))))
            cart    <- service.get(userId)
          } yield cart

          result.asserting(_ mustBe Cart(List(CartItem(itemId1, Item.Quantity(8)), CartItem(itemId2, Item.Quantity(4)))))
        }
      }
    }

    "update" - {
      "update item amount in cart" in {
        withRedisCommands { redis =>
          val result = for {
            service <- CartService.redisCartService(redis, shopConfig)
            _       <- service.add(userId, Cart(List(CartItem(itemId1, Item.Quantity(4)))))
            _       <- service.update(userId, Cart(List(CartItem(itemId1, Item.Quantity(2)))))
            cart    <- service.get(userId)
          } yield cart

          result.asserting(_ mustBe Cart(List(CartItem(itemId1, Item.Quantity(2)))))
        }
      }

      "not do anything if item not found" in {
        withRedisCommands { redis =>
          val result = for {
            service <- CartService.redisCartService(redis, shopConfig)
            _       <- service.update(userId, Cart(List(CartItem(itemId1, Item.Quantity(2)))))
            cart    <- service.get(userId)
          } yield cart

          result.asserting(_ mustBe Cart(Nil))
        }
      }
    }

    "removeItem" - {
      "remove item from cart" in {
        withRedisCommands { redis =>
          val result = for {
            service <- CartService.redisCartService(redis, shopConfig)
            _       <- service.add(userId, Cart(List(CartItem(itemId1, Item.Quantity(4)))))
            _       <- service.removeItem(userId, itemId1)
            cart    <- service.get(userId)
          } yield cart

          result.asserting(_ mustBe Cart(Nil))
        }
      }

      "not do anything if item not found" in {
        withRedisCommands { redis =>
          val result = for {
            service <- CartService.redisCartService(redis, shopConfig)
            _       <- service.removeItem(userId, itemId1)
            cart    <- service.get(userId)
          } yield cart

          result.asserting(_ mustBe Cart(Nil))
        }
      }
    }

    "delete" - {
      "delete cart" in {
        withRedisCommands { redis =>
          val result = for {
            service <- CartService.redisCartService(redis, shopConfig)
            _       <- service.add(userId, Cart(List(CartItem(itemId1, Item.Quantity(4)))))
            _       <- service.delete(userId)
            cart    <- service.get(userId)
          } yield cart

          result.asserting(_ mustBe Cart(Nil))
        }
      }
    }
  }
}
