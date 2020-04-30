package io.kirill.shoppingcart.shop.item

import java.util.UUID

import cats.effect.IO
import io.kirill.shoppingcart.PostgresRepositorySpec
import io.kirill.shoppingcart.common.errors.{ForeignKeyViolation, SqlConstraintViolation}
import io.kirill.shoppingcart.shop.brand.{BrandId, BrandName, BrandRepository}
import io.kirill.shoppingcart.shop.category.{CategoryId, CategoryName, CategoryRepository}
import squants.market.GBP

class ItemRepositorySpec extends PostgresRepositorySpec {

  "An ItemRepository" - {

    "find" - {
      "find created item by id" in {
        val itemRepository = ItemRepository.make(session)

        val result = for {
          repo <- itemRepository
          iid <- insertTestItem(repo)
          item <- repo.find(iid)
        } yield item.get

        result.asserting { item =>
          item.description must be (ItemDescription("description"))
          item.price must be (GBP(BigDecimal(10.99)))
          item.brand.name must be (BrandName("test-brand"))
          item.category.name must be (CategoryName("test-category"))
        }
      }

      "return empty option if item does not exist" in {
        val itemRepository = ItemRepository.make(session)

        val result = itemRepository.flatMap(r => r.find(ItemId(UUID.randomUUID())))

        result.asserting(_ must be (None))
      }
    }

    "findBy" - {
      "find item by brand name" in {
        val itemRepository = ItemRepository.make(session)

        val result = for {
          repo <- itemRepository
          _ <- insertTestItem(repo)
          items <- repo.findBy(BrandName("test-brand"))
        } yield items

        result.asserting { items =>
          items must not be empty
          items.map(_.brand.name) must contain only (BrandName("test-brand"))
        }
      }

      "return empty list if no matches" in {
        val itemRepository = ItemRepository.make(session)

        itemRepository.flatMap(_.findBy(BrandName("foo"))).asserting(_ must be (Nil))
      }
    }

    "findAll" - {
      "return all items from repo" in {
        val itemRepository = ItemRepository.make(session)

        val result = for {
          repo <- itemRepository
          _ <- insertTestItem(repo)
          items <- repo.findAll
        } yield items

        result.asserting { items =>
          items must not be empty
          items.map(_.brand.name) must contain only (BrandName("test-brand"))
        }
      }
    }

    "update" - {
      "change item's price" in {
        val itemRepository = ItemRepository.make(session)

        val result = for {
          repo <- itemRepository
          id <- insertTestItem(repo)
          _ <- repo.update(UpdateItem(id, GBP(BigDecimal(99.99))))
          item <- repo.find(id)
        } yield item.get

        result.asserting { item =>
          item.price must be (GBP(BigDecimal(99.99)))
        }
      }
    }

    "create" - {
      "return error when brand does not exist" in {
        val result = for {
          repo <- ItemRepository.make(session)
          iid <- repo.create(CreateItem(ItemName("item"), ItemDescription("description"), GBP(BigDecimal(10.99)), BrandId(UUID.randomUUID()), CategoryId(UUID.randomUUID())))
        } yield iid

        result.assertThrows[ForeignKeyViolation]
      }
    }
  }

  def insertTestBrand: IO[BrandId] =
    for {
      r <- BrandRepository.make(session)
      bs <- r.findAll
      bid <- bs.find(_.name == BrandName("test-brand")).fold(r.create(BrandName("test-brand")))(b => IO.pure(b.id))
    } yield bid

  def insertTestCategory: IO[CategoryId] =
    for {
      r <- CategoryRepository.make(session)
      bs <- r.findAll
      cid <- bs.find(_.name == CategoryName("test-category")).fold(r.create(CategoryName("test-category")))(b => IO.pure(b.id))
    } yield cid

  def insertTestItem(repo: ItemRepository[IO]): IO[ItemId] =
    for {
      bid <- insertTestBrand
      cid <- insertTestCategory
      iid <- repo.create(CreateItem(ItemName(s"item-${System.currentTimeMillis()}"), ItemDescription("description"), GBP(BigDecimal(10.99)), bid, cid))
    } yield iid
}
