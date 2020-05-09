package io.kirill.shoppingcart.shop.item

import cats.effect.IO
import io.kirill.shoppingcart.common.errors.ItemNotFound
import io.kirill.shoppingcart.shop.brand.BrandName
import org.mockito.scalatest.AsyncMockitoSugar
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import squants.market.GBP

class ItemServiceSpec extends AsyncFreeSpec with Matchers with AsyncMockitoSugar {

  val item1: Item = ItemBuilder.item("item-1")
  val item2: Item = ItemBuilder.item("item-2")

  "An ItemService" - {

    "findAll" - {
      "should stream all items from repo" in {
        val result = for {
          repo <- repoMock
          _ = when(repo.findAll).thenReturn(fs2.Stream(item1, item2).lift[IO])
          service <- ItemService.make(repo)
          items   <- service.findAll.compile.toList
        } yield items

        result.unsafeToFuture().map(_ must be(List(item1, item2)))
      }
    }

    "findBy" - {
      "should stream items by brand" in {
        val result = for {
          repo <- repoMock
          _ = when(repo.findBy(BrandName("brand"))).thenReturn(fs2.Stream(item1, item2).lift[IO])
          service <- ItemService.make(repo)
          items   <- service.findBy(BrandName("brand")).compile.toList
        } yield items

        result.unsafeToFuture().map(_ must be(List(item1, item2)))
      }
    }

    "find" - {
      "should find item by id" in {
        val result = for {
          repo <- repoMock
          _ = when(repo.find(item1.id)).thenReturn(IO.pure(Some(item1)))
          service <- ItemService.make(repo)
          item    <- service.findById(item1.id)
        } yield item

        result.unsafeToFuture().map(_ must be(item1))
      }

      "should return ItemNotFound error" in {
        val result = for {
          repo <- repoMock
          _ = when(repo.find(item1.id)).thenReturn(IO.pure(None))
          service <- ItemService.make(repo)
          item    <- service.findById(item1.id)
        } yield item

        recoverToSucceededIf[ItemNotFound] {
          result.unsafeToFuture()
        }
      }
    }

    "update" - {
      "should update item" in {
        val result = for {
          repo <- repoMock
          _ = when(repo.update(any[UpdateItem])).thenReturn(IO.pure(()))
          service <- ItemService.make(repo)
          res    <- service.update(UpdateItem(item1.id, GBP(99.99)))
        } yield res

        result.unsafeToFuture().map { res =>
          res must be(())
        }
      }
    }

    "create" - {
      "should create item" in {
        val result = for {
          repo <- repoMock
          _ = when(repo.create(any[CreateItem])).thenReturn(IO.pure(item1.id))
          service <- ItemService.make(repo)
          res    <- service.create(CreateItem(item1.name, item1.description, item1.price, item1.brand.id, item1.category.id))
        } yield res

        result.unsafeToFuture().map { res =>
          res must be(item1.id)
        }
      }
    }
  }

  def repoMock: IO[ItemRepository[IO]] = IO(mock[ItemRepository[IO]])
}
