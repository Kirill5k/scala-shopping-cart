package io.kirill.shoppingcart.shop.item

import cats.effect.IO
import io.kirill.shoppingcart.PostgresRepositorySpec
import io.kirill.shoppingcart.common.errors.SqlConstraintViolation

class ItemRepositorySpec extends PostgresRepositorySpec {

  "An ItemRepository" - {

    "create new items" in {
      val repository = ItemRepository.make(session)
    }
  }
}
