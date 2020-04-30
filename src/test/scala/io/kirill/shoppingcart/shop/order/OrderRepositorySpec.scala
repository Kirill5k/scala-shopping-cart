package io.kirill.shoppingcart.shop.order

import java.util.UUID

import cats.effect.IO
import io.kirill.shoppingcart.PostgresRepositorySpec
import io.kirill.shoppingcart.auth.UserId
import io.kirill.shoppingcart.common.errors.ForeignKeyViolation
import io.kirill.shoppingcart.shop.brand.{BrandId, BrandName, BrandRepository}
import io.kirill.shoppingcart.shop.category.{CategoryId, CategoryName, CategoryRepository}
import squants.market.GBP

class OrderRepositorySpec extends PostgresRepositorySpec {

  "An OrderRepository" - {

    "find" - {
      "return empty option if order does not exist" in {
        val orderRepository = OrderRepository.make(session)

        val result = orderRepository.flatMap(r => r.find(OrderId(UUID.randomUUID())))

        result.asserting(_ must be(None))
      }
    }

    "findBy" - {
      "return empty list if no matches" in {
        val orderRepository = OrderRepository.make(session)

        orderRepository.flatMap(_.findBy(UserId(UUID.randomUUID()))).asserting(_ must be(Nil))
      }
    }
  }
}
