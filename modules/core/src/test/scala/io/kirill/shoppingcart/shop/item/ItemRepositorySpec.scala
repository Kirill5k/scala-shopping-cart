package io.kirill.shoppingcart.shop.item

import java.util.UUID

import cats.effect.IO
import io.kirill.shoppingcart.PostgresRepositorySpec
import io.kirill.shoppingcart.common.errors.{ForeignKeyViolation, UniqueViolation}
import io.kirill.shoppingcart.shop.brand.{Brand}
import io.kirill.shoppingcart.shop.category.{Category, CategoryRepository}
import squants.market.GBP

class ItemRepositorySpec extends PostgresRepositorySpec {

  "An ItemRepository" - {

    "find" - {
      "find created item by id" in {
        val itemRepository = ItemRepository.make(session)

        val result = for {
          repo <- itemRepository
          iid  <- insertTestItem(repo)
          item <- repo.find(iid)
        } yield item.get

        result.asserting { item =>
          item.description must be(Item.Description("description"))
          item.price must be(GBP(BigDecimal(10.99)))
          item.brand.name must be(Brand.Name("test-brand"))
          item.category.name must be(Category.Name("test-category"))
        }
      }

      "return empty option if item does not exist" in {
        val itemRepository = ItemRepository.make(session)

        val result = itemRepository.flatMap(r => r.find(Item.Id(UUID.randomUUID())))

        result.asserting(_ must be(None))
      }
    }

    "findBy" - {
      "find item by brand name" in {
        val itemRepository = ItemRepository.make(session)

        val result = for {
          repo  <- itemRepository
          _     <- insertTestItem(repo)
          items <- repo.findBy(Brand.Name("test-brand")).compile.toList
        } yield items

        result.asserting { items =>
          items must not be empty
          items.map(_.brand.name) must contain only (Brand.Name("test-brand"))
        }
      }

      "return empty list if no matches" in {
        val itemRepository = ItemRepository.make(session)

        itemRepository.flatMap(_.findBy(Brand.Name("foo")).compile.toList).asserting(_ must be(Nil))
      }
    }

    "findAll" - {
      "return all items from repo" in {
        val itemRepository = ItemRepository.make(session)

        val result = for {
          repo  <- itemRepository
          _     <- insertTestItem(repo)
          items <- repo.findAll.compile.toList
        } yield items

        result.asserting { items =>
          items must not be empty
          items.map(_.brand.name) must contain only (Brand.Name("test-brand"))
        }
      }
    }

    "update" - {
      "change item's price" in {
        val itemRepository = ItemRepository.make(session)

        val result = for {
          repo <- itemRepository
          id   <- insertTestItem(repo)
          _    <- repo.update(UpdateItem(id, GBP(BigDecimal(99.99))))
          item <- repo.find(id)
        } yield item.get

        result.asserting { item =>
          item.price must be(GBP(BigDecimal(99.99)))
        }
      }
    }

    "create" - {
      "return error when brand does not exist" in {
        val result = for {
          repo <- ItemRepository.make(session)
          iid <- repo.create(
            CreateItem(
              Item.Name("item"),
              Item.Description("description"),
              GBP(BigDecimal(10.99)),
              Brand.Id(UUID.randomUUID()),
              Category.Id(UUID.randomUUID())
            )
          )
        } yield iid

        result.assertThrows[ForeignKeyViolation]
      }
    }

    "exists" - {
      "return true when item exists" in {
        val itemRepository = ItemRepository.make(session)

        val result = for {
          repo   <- itemRepository
          id     <- insertTestItem(repo)
          exists <- repo.exists(id)
        } yield exists

        result.asserting { res =>
          res must be(true)
        }
      }

      "return false when item does not exist" in {
        val itemRepository = ItemRepository.make(session)

        val result = for {
          repo   <- itemRepository
          exists <- repo.exists(Item.Id(UUID.randomUUID()))
        } yield exists

        result.asserting { res =>
          res must be(false)
        }
      }
    }
  }

  def insertTestBrand: IO[Brand.Id] =
    for {
      r   <- BrandRepository.make(session)
      bs  <- r.findAll.compile.toList
      bid <- bs.find(_.name == Brand.Name("test-brand")).fold(r.create(Brand.Name("test-brand")))(b => IO.pure(b.id))
    } yield bid

  def insertTestCategory: IO[Category.Id] =
    for {
      r   <- CategoryRepository.make(session)
      bs  <- r.findAll.compile.toList
      cid <- bs.find(_.name == Category.Name("test-category")).fold(r.create(Category.Name("test-category")))(b => IO.pure(b.id))
    } yield cid

  def insertTestItem(repo: ItemRepository[IO]): IO[Item.Id] =
    for {
      bid <- insertTestBrand
      cid <- insertTestCategory
      iid <- repo.create(
        CreateItem(Item.Name(s"item-${System.currentTimeMillis()}"), Item.Description("description"), GBP(BigDecimal(10.99)), bid, cid)
      )
    } yield iid
}
