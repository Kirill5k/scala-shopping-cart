package io.kirill.shoppingcart.shop.cart

import java.util.UUID

import io.kirill.shoppingcart.RedisSpec
import io.kirill.shoppingcart.auth.user.UserId
import io.kirill.shoppingcart.shop.item.ItemId

class CartServiceSpec extends RedisSpec {

  val userId = UserId(UUID.randomUUID())
  val itemId1 = ItemId(UUID.randomUUID())
  val itemId2 = ItemId(UUID.randomUUID())

  "A RedisCartService" - {

    "add" - {
      "add items to a cart" in {
        withRedisAsync() { port =>
          val result = stringCommands(port).use { r =>
            for {
              service <- CartService.redisCartService(r)
              _ <- service.add(userId, Cart(List(CartItem(itemId1, Quantity(4)))))
              _ <- service.add(userId, Cart(List(CartItem(itemId1, Quantity(4)), CartItem(itemId2, Quantity(4)))))
              cart <- service.get(userId)
            } yield cart
          }

          result.asserting(_ must be (Cart(List(CartItem(itemId1, Quantity(8)), CartItem(itemId2, Quantity(4))))))
        }
      }
    }

    "update" - {
      "update item amount in cart" in {
        withRedisAsync() { port =>
          val result = stringCommands(port).use(r => for {
            service <- CartService.redisCartService(r)
            _ <- service.add(userId, Cart(List(CartItem(itemId1, Quantity(4)))))
            _ <- service.update(userId, Cart(List(CartItem(itemId1, Quantity(2)))))
            cart <- service.get(userId)
          } yield cart)

          result.asserting(_ must be (Cart(List(CartItem(itemId1, Quantity(2))))))
        }
      }

      "not do anything if item not found" in {
        withRedisAsync() { port =>
          val result = stringCommands(port).use(r => for {
            service <- CartService.redisCartService(r)
            _ <- service.update(userId, Cart(List(CartItem(itemId1, Quantity(2)))))
            cart <- service.get(userId)
          } yield cart)

          result.asserting(_ must be (Cart(Nil)))
        }
      }
    }

    "removeItem" - {
      "remove item from cart" in {
        withRedisAsync() { port =>
          val result = stringCommands(port).use(r => for {
            service <- CartService.redisCartService(r)
            _ <- service.add(userId, Cart(List(CartItem(itemId1, Quantity(4)))))
            _ <- service.removeItem(userId, itemId1)
            cart <- service.get(userId)
          } yield cart)

          result.asserting(_ must be (Cart(Nil)))
        }
      }

      "not do anything if item not found" in {
        withRedisAsync() { port =>
          val result = stringCommands(port).use(r => for {
            service <- CartService.redisCartService(r)
            _ <- service.removeItem(userId, itemId1)
            cart <- service.get(userId)
          } yield cart)

          result.asserting(_ must be (Cart(Nil)))
        }
      }
    }

    "delete" - {
      "delete cart" in {
        withRedisAsync() { port =>
          val result = stringCommands(port).use(r => for {
            service <- CartService.redisCartService(r)
            _ <- service.add(userId, Cart(List(CartItem(itemId1, Quantity(4)))))
            _ <- service.delete(userId)
            cart <- service.get(userId)
          } yield cart)

          result.asserting(_ must be (Cart(Nil)))
        }
      }
    }
  }
}
