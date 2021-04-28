package io.kirill.shoppingcart.shop.category

import cats.effect.IO
import io.kirill.shoppingcart.PostgresRepositorySpec
import io.kirill.shoppingcart.common.errors.UniqueViolation

class CategoryRepositorySpec extends PostgresRepositorySpec {

  "A CategoryRepository" - {

    "create new brands and search them" in {
      val repository = CategoryRepository.make[IO](session)

      val allCategories = for {
        r      <- repository
        _      <- r.create(Category.Name("foo"))
        brands <- r.findAll.compile.toList
      } yield brands

      allCategories.asserting { brands =>
        brands must have size 1
        brands.head.name must be(Category.Name("foo"))
      }
    }

    "return error if brand with same name already exists" in {
      val repository = CategoryRepository.make[IO](session)

      val error = for {
        r <- repository
        _ <- r.create(Category.Name("b1"))
        e <- r.create(Category.Name("b1"))
      } yield e

      error.assertThrows[UniqueViolation]
    }
  }
}
