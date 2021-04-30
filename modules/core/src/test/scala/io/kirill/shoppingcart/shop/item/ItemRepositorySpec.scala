package io.kirill.shoppingcart.shop.item

import java.util.UUID
import cats.effect.IO
import cats.implicits._
import io.kirill.shoppingcart.PostgresRepositorySpec
import io.kirill.shoppingcart.common.errors.{ForeignKeyViolation, UniqueViolation}
import io.kirill.shoppingcart.shop.brand.{Brand, BrandRepository}
import io.kirill.shoppingcart.shop.category.{Category, CategoryRepository}
import squants.market.GBP

class ItemRepositorySpec extends PostgresRepositorySpec {

  val testBrand    = Brand.Name("test-brand")
  val testCategory = Category.Name("test-category")

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
          item.description mustBe Item.Description("description")
          item.price mustBe GBP(BigDecimal(10.99))
          item.brand.name mustBe testBrand
          item.category.name mustBe testCategory
        }
      }

      "return empty option if item does not exist" in {
        val itemRepository = ItemRepository.make(session)

        val result = itemRepository.flatMap(r => r.find(Item.Id(UUID.randomUUID())))

        result.asserting(_ mustBe None)
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
          items.map(_.brand.name) must contain only Brand.Name("test-brand")
        }
      }

      "return empty list if no matches" in {
        val itemRepository = ItemRepository.make(session)

        itemRepository.flatMap(_.findBy(Brand.Name("foo")).compile.toList).asserting(_ mustBe Nil)
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
          items.map(_.brand.name) must contain only Brand.Name("test-brand")
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
          item.price mustBe GBP(BigDecimal(99.99))
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

        result.asserting(_ mustBe true)
      }

      "return false when item does not exist" in {
        val itemRepository = ItemRepository.make(session)

        val result = for {
          repo   <- itemRepository
          exists <- repo.exists(Item.Id(UUID.randomUUID()))
        } yield exists

        result.asserting(_ mustBe false)
      }
    }
  }

  def insertTestBrand: IO[Brand.Id] =
    for {
      r   <- BrandRepository.make(session)
      bs  <- r.findAll.compile.toList
      bid <- bs.find(_.name == testBrand).fold(r.create(testBrand))(_.id.pure[IO])
    } yield bid

  def insertTestCategory: IO[Category.Id] =
    for {
      r   <- CategoryRepository.make(session)
      bs  <- r.findAll.compile.toList
      cid <- bs.find(_.name == testCategory).fold(r.create(testCategory))(_.id.pure[IO])
    } yield cid

  def insertTestItem(repo: ItemRepository[IO]): IO[Item.Id] =
    for {
      bid <- insertTestBrand
      cid <- insertTestCategory
      createItem = CreateItem(
        Item.Name(s"item-${System.currentTimeMillis()}"),
        Item.Description("description"),
        GBP(BigDecimal(10.99)),
        bid,
        cid
      )
      iid <- repo.create(createItem)
    } yield iid
}
