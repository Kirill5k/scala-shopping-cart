package io.kirill.shoppingcart.shop.category

import java.util.UUID

import cats.effect.IO
import io.kirill.shoppingcart.common.errors.{CategoryAlreadyExists, UniqueViolation}
import org.mockito.scalatest.AsyncMockitoSugar
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers

class CategoryServiceSpec extends AsyncFreeSpec with Matchers with AsyncMockitoSugar {

  val category1 = Category(Category.Id(UUID.randomUUID()), Category.Name("category-1"))
  val category2 = Category(Category.Id(UUID.randomUUID()), Category.Name("category-2"))

  "A CategoryService" - {
    "findAll" - {
      "should stream all categories from repo" in {
        val result = for {
          repo <- repoMock
          _ = when(repo.findAll).thenReturn(fs2.Stream(category1, category2).lift[IO])
          service <- CategoryService.make[IO](repo)
          items   <- service.findAll.compile.toList
        } yield items

        result.unsafeToFuture().map(_ must be(List(category1, category2)))
      }
    }

    "create" - {
      "should create new brand" in {
        val result = for {
          repo <- repoMock
          _ = when(repo.create(category1.name)).thenReturn(IO.pure(category1.id))
          service <- CategoryService.make[IO](repo)
          id      <- service.create(category1.name)
        } yield id

        result.unsafeToFuture().map(_ must be(category1.id))
      }

      "should return category already exists error if unique violation" in {
        val result = for {
          repo <- repoMock
          _ = when(repo.create(category1.name)).thenReturn(IO.raiseError(UniqueViolation("error")))
          service <- CategoryService.make[IO](repo)
          id      <- service.create(category1.name)
        } yield id

        recoverToSucceededIf[CategoryAlreadyExists] {
          result.unsafeToFuture()
        }
      }
    }
  }

  def repoMock: IO[CategoryRepository[IO]] = IO(mock[CategoryRepository[IO]])
}
