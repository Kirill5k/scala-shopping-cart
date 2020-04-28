package io.kirill.shoppingcart.shop.category

import cats.effect.IO
import io.kirill.shoppingcart.PostgresRepositorySpec
import io.kirill.shoppingcart.common.errors.SqlConstraintViolation

class CategoryRepositorySpec extends PostgresRepositorySpec {

  "A CategoryRepository" - {

    "create new brands and search them" in {
      val repository = CategoryRepository.make[IO](session)

      val allCategories = for {
        r <- repository
        _ <- r.create(CategoryName("foo"))
        brands <- r.findAll
      } yield brands

      allCategories.asserting { brands =>
        brands must have size 1
        brands.head.name must be (CategoryName("foo"))
      }
    }

    "return error if brand with same name already exists" in {
      val repository = CategoryRepository.make[IO](session)

      val error = for {
        r <- repository
        _ <- r.create(CategoryName("b1"))
        e <- r.create(CategoryName("b1"))
      } yield e

      error.assertThrows[SqlConstraintViolation]
    }
  }
}
