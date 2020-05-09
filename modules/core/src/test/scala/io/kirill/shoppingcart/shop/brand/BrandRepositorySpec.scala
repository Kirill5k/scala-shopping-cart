package io.kirill.shoppingcart.shop.brand

import cats.effect.IO
import cats.implicits._
import io.kirill.shoppingcart.PostgresRepositorySpec
import io.kirill.shoppingcart.common.errors.UniqueViolation

class BrandRepositorySpec extends PostgresRepositorySpec {

  "A BrandRepository" - {

    "create new brands and search them" in {
      val repository = BrandRepository.make[IO](session)

      val allBrands = for {
        r <- repository
        _ <- r.create(BrandName("foo"))
        brands <- r.findAll.compile.toList
      } yield brands

      allBrands.asserting { brands =>
        brands must have size 1
        brands.head.name must be (BrandName("foo"))
      }
    }

    "return error if brand with same name already exists" in {
      val repository = BrandRepository.make[IO](session)

      val error = for {
        r <- repository
        _ <- r.create(BrandName("b1"))
        e <- r.create(BrandName("b1"))
      } yield e

      error.assertThrows[UniqueViolation]
    }
  }
}
