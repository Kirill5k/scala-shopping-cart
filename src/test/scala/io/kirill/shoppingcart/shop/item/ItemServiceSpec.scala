package io.kirill.shoppingcart.shop.item

import cats.effect.IO
import cats.implicits._
import io.kirill.shoppingcart.CatsIOSpec
import io.kirill.shoppingcart.common.errors.ItemNotFound
import io.kirill.shoppingcart.shop.brand.BrandName
import squants.market.GBP

class ItemServiceSpec extends CatsIOSpec {

  val item1: Item = ItemBuilder.item("item-1")
  val item2: Item = ItemBuilder.item("item-2")

  "An ItemService" - {

    "findAll" - {
      "should stream all items from repo" in {
        val repo = repoMock
        when(repo.findAll).thenReturn(fs2.Stream(item1, item2).lift[IO])
        val result = for {
          service <- ItemService.make(repo)
          items <- service.findAll.compile.toList
        } yield items

        result.asserting(_ must be (List(item1, item2)))
      }
    }

    "findBy" - {
      "should stream items by brand" in {
        val repo = repoMock
        when(repo.findBy(BrandName("brand"))).thenReturn(fs2.Stream(item1, item2).lift[IO])
        val result = for {
          service <- ItemService.make(repo)
          items <- service.findBy(BrandName("brand")).compile.toList
        } yield items

        result.asserting(_ must be (List(item1, item2)))
      }
    }

    "find" - {
      "should find item by id" in {
        val repo = repoMock
        when(repo.find(item1.id)).thenReturn(IO.pure(Some(item1)))
        val result = for {
          service <- ItemService.make(repo)
          item <- service.findById(item1.id)
        } yield item

        result.asserting(_ must be (item1))
      }

      "should return ItemNotFound error" in {
        val repo = repoMock
        when(repo.find(item1.id)).thenReturn(IO.pure(None))
        val result = for {
          service <- ItemService.make(repo)
          item <- service.findById(item1.id)
        } yield item

        result.assertThrows[ItemNotFound]
      }
    }

    "update" - {
      "should update item" in {
        val repo = repoMock
        when(repo.update(any[UpdateItem])).thenReturn(IO.pure(()))
        val result = for {
          service <- ItemService.make(repo)
          item <- service.update(UpdateItem(item1.id, GBP(99.99)))
        } yield item

        result.asserting { r =>
          verify(repo).update(UpdateItem(item1.id, GBP(99.99)))
          r must be (())
        }
      }
    }

    "create" - {
      "should create item" in {
        val repo = repoMock
        when(repo.create(any[CreateItem])).thenReturn(IO.pure(item1.id))
        val result = for {
          service <- ItemService.make(repo)
          item <- service.create(CreateItem(item1.name, item1.description, item1.price, item1.brand.id, item1.category.id))
        } yield item

        result.asserting { r =>
          verify(repo).create(CreateItem(item1.name, item1.description, item1.price, item1.brand.id, item1.category.id))
          r must be (item1.id)
        }
      }
    }
  }

  def repoMock: ItemRepository[IO] = mock[ItemRepository[IO]]
}
