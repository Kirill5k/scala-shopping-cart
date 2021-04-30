package io.kirill.shoppingcart.shop.item

import cats.effect.IO
import io.kirill.shoppingcart.common.errors.ItemNotFound
import io.kirill.shoppingcart.shop.brand.Brand
import org.mockito.scalatest.AsyncMockitoSugar
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import squants.market.GBP
import fs2.Stream

class ItemServiceSpec extends AsyncFreeSpec with Matchers with AsyncMockitoSugar {

  val item1: Item = ItemBuilder.item("item-1")
  val item2: Item = ItemBuilder.item("item-2")

  "An ItemService" - {

    "findAll" - {
      "should stream all items from repo" in {
        val repo = mock[ItemRepository[IO]]
        when(repo.findAll).thenReturn(Stream(item1, item2).lift[IO])

        val result = for {
          service <- ItemService.make(repo)
          items   <- service.findAll.compile.toList
        } yield items

        result.unsafeToFuture().map(_ mustBe List(item1, item2))
      }
    }

    "findBy" - {
      "should stream items by brand" in {
        val repo = mock[ItemRepository[IO]]
        when(repo.findBy(Brand.Name("brand"))).thenReturn(Stream(item1, item2).lift[IO])

        val result = for {
          service <- ItemService.make(repo)
          items   <- service.findBy(Brand.Name("brand")).compile.toList
        } yield items

        result.unsafeToFuture().map(_ mustBe List(item1, item2))
      }
    }

    "find" - {
      "should find item by id" in {
        val repo = mock[ItemRepository[IO]]
        when(repo.find(item1.id)).thenReturn(IO.pure(Some(item1)))

        val result = for {
          service <- ItemService.make(repo)
          item    <- service.findById(item1.id)
        } yield item

        result.unsafeToFuture().map(_ mustBe item1)
      }

      "should return ItemNotFound error when item does not exist" in {
        val repo = mock[ItemRepository[IO]]
        when(repo.find(item1.id)).thenReturn(IO.pure(None))

        val result = for {
          service <- ItemService.make(repo)
          item    <- service.findById(item1.id)
        } yield item

        result.attempt.unsafeToFuture().map(_ mustBe Left(ItemNotFound(item1.id)))
      }
    }

    "update" - {
      "should update item" in {
        val repo = mock[ItemRepository[IO]]
        when(repo.exists(any[Item.Id])).thenReturn(IO.pure(true))
        when(repo.update(any[UpdateItem])).thenReturn(IO.unit)

        val result = for {
          service <- ItemService.make(repo)
          res     <- service.update(UpdateItem(item1.id, GBP(99.99)))
        } yield res

        result.unsafeToFuture().map(_ mustBe ())
      }

      "should return item not found error when item does not exists" in {
        val repo = mock[ItemRepository[IO]]
        when(repo.exists(any[Item.Id])).thenReturn(IO.pure(false))

        val result = for {
          service <- ItemService.make(repo)
          res     <- service.update(UpdateItem(item1.id, GBP(99.99)))
        } yield res

        result.attempt.unsafeToFuture().map(_ mustBe Left(ItemNotFound(item1.id)))
      }
    }

    "create" - {
      "should create item" in {
        val repo = mock[ItemRepository[IO]]
        when(repo.create(any[CreateItem])).thenReturn(IO.pure(item1.id))

        val result = for {
          service <- ItemService.make(repo)
          res     <- service.create(CreateItem(item1.name, item1.description, item1.price, item1.brand.id, item1.category.id))
        } yield res

        result.unsafeToFuture().map(_ mustBe item1.id)
      }
    }
  }
}
