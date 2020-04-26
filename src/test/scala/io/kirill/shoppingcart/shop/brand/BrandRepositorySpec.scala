package io.kirill.shoppingcart.shop.brand

import cats.effect.IO
import io.kirill.shoppingcart.PostgresRepositorySpec
import org.scalatest.freespec.AsyncFreeSpec

class BrandRepositorySpec extends PostgresRepositorySpec {

  "A BrandRepository" - {

    "create new brands" in {
      val repository = BrandRepository[IO]

      repository.create(BrandName("foo")).asserting(_.name must be (BrandName("foo")))
    }
  }
}
