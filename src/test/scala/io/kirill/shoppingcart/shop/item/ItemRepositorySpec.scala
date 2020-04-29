package io.kirill.shoppingcart.shop.item

import java.util.UUID

import cats.effect.IO
import io.kirill.shoppingcart.PostgresRepositorySpec
import io.kirill.shoppingcart.common.errors.SqlConstraintViolation
import io.kirill.shoppingcart.shop.brand.{BrandId, BrandName, BrandRepository}
import io.kirill.shoppingcart.shop.category.{CategoryId, CategoryName, CategoryRepository}
import squants.market.GBP

class ItemRepositorySpec extends PostgresRepositorySpec {

  "An ItemRepository" - {

    "create new item and find it by id" in {
      val itemRepository = ItemRepository.make(session)

      val result = for {
        bid <- insertTestBrand
        cid <- insertTestCategory
        repo <- itemRepository
        iid <- repo.create(CreateItem(ItemName("item"), ItemDescription("description"), GBP(BigDecimal(10.99)), bid, cid))
        item <- repo.find(iid)
      } yield item.get

      result.asserting { item =>
        item.name must be (ItemName("item"))
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

  def insertTestBrand: IO[BrandId] =
    BrandRepository.make(session).flatMap(r => r.create(BrandName("test-brand")))

  def insertTestCategory: IO[CategoryId] =
    CategoryRepository.make(session).flatMap(r => r.create(CategoryName("test-category")))
}
