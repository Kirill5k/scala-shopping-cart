package io.kirill.shoppingcart.shop.brand

import cats.effect.IO
import cats.implicits._
import io.kirill.shoppingcart.PostgresRepositorySpec
import io.kirill.shoppingcart.common.errors.SqlConstraintViolation

class BrandRepositorySpec extends PostgresRepositorySpec {

  "A BrandRepository" - {

    "create new brands and search them" in {
      val repository = BrandRepository[IO]

      repository.create(BrandName("foo")) *> repository.findAll.asserting { brands =>
        brands must have size 1
        brands.head.name must be (BrandName("foo"))
      }
    }

    "return error if brand with same name already exists" in {
      val repository = BrandRepository[IO]

      repository.create(BrandName("b1")) *> repository.create(BrandName("b1")).assertThrows[SqlConstraintViolation]
    }
  }
}
