package io.kirill.shoppingcart.shop.brand

import java.util.UUID

import cats.effect.IO
import org.mockito.scalatest.AsyncMockitoSugar
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers

class BrandServiceSpec extends AsyncFreeSpec with Matchers with AsyncMockitoSugar {

  val brand1 = Brand(BrandId(UUID.randomUUID()), BrandName("brand-1"))
  val brand2 = Brand(BrandId(UUID.randomUUID()), BrandName("brand-2"))

  "A BrandService" - {
    "findAll" - {
      "should stream all categories from repo" in {
        val result = for {
          repo <- repoMock
          _ = when(repo.findAll).thenReturn(fs2.Stream(brand1, brand2).lift[IO])
          service <- BrandService.make[IO](repo)
          items   <- service.findAll.compile.toList
        } yield items

        result.unsafeToFuture().map(_ must be(List(brand1, brand2)))
      }
    }

    "create" - {
      "should create new category" in {
        val result = for {
          repo <- repoMock
          _ = when(repo.create(brand1.name)).thenReturn(IO.pure(brand1.id))
          service <- BrandService.make[IO](repo)
          id      <- service.create(brand1.name)
        } yield id

        result.unsafeToFuture().map(_ must be (brand1.id))
      }
    }
  }

  def repoMock: IO[BrandRepository[IO]] = IO(mock[BrandRepository[IO]])
}
