package io.kirill.shoppingcart.shop.item

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.literal._
import io.kirill.shoppingcart.ControllerSpec
import io.kirill.shoppingcart.common.errors.ItemNotFound
import io.kirill.shoppingcart.common.json._
import io.kirill.shoppingcart.common.web.ErrorResponse
import io.kirill.shoppingcart.shop.brand.Brand
import io.kirill.shoppingcart.shop.category.Category
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import squants.market.GBP

import java.util.UUID

class ItemControllerSpec extends ControllerSpec {
  import ItemBuilder._
  import ItemController._

  val itemId = UUID.fromString("607995e0-8e3a-11ea-bc55-0242ac130003")

  "An ItemController" when {

    "GET /items/{id}" should {
      "find item by id" in {
        val itemServiceMock = mock[ItemService[IO]]
        val controller      = new ItemController[IO](itemServiceMock)

        val testitem = item("item-1").copy(id = Item.Id(itemId))
        when(itemServiceMock.findById(any[Item.Id])).thenReturn(IO.pure(testitem))

        val request  = Request[IO](uri = uri"/items/607995e0-8e3a-11ea-bc55-0242ac130003")
        val response = controller.routes(adminMiddleware).orNotFound.run(request)

        val expectedResponse = ItemResponse(
          testitem.id,
          testitem.name,
          testitem.description,
          testitem.price,
          testitem.brand.name,
          testitem.category.name
        )
        verifyResponse[ItemResponse](response, Status.Ok, Some(expectedResponse))
        verify(itemServiceMock).findById(testitem.id)
      }

      "return 404 if item does not exist" in {
        val itemServiceMock = mock[ItemService[IO]]
        val controller      = new ItemController[IO](itemServiceMock)

        when(itemServiceMock.findById(any[Item.Id])).thenReturn(IO.raiseError(ItemNotFound(Item.Id(itemId))))

        val request  = Request[IO](uri = uri"/items/607995e0-8e3a-11ea-bc55-0242ac130003")
        val response = controller.routes(adminMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](
          response,
          Status.NotFound,
          Some(ErrorResponse("Item with id 607995e0-8e3a-11ea-bc55-0242ac130003 does not exist"))
        )
        verify(itemServiceMock).findById(Item.Id(itemId))
      }
    }

    "GET /items" should {
      "return list of items on GET" in {
        val itemServiceMock = mock[ItemService[IO]]
        val controller      = new ItemController[IO](itemServiceMock)

        val testitem = item("item-1")
        when(itemServiceMock.findAll).thenReturn(fs2.Stream.emits(List(testitem)).lift[IO])

        val request  = Request[IO](uri = uri"/items")
        val response = controller.routes(adminMiddleware).orNotFound.run(request)

        val expectedResponse = ItemResponse(
          testitem.id,
          testitem.name,
          testitem.description,
          testitem.price,
          testitem.brand.name,
          testitem.category.name
        )
        verifyResponse[List[ItemResponse]](response, Status.Ok, Some(List(expectedResponse)))
        verify(itemServiceMock).findAll
      }
    }

    "GET /items?brand=x" should {
      "return list of items on GET by brand" in {
        val itemServiceMock = mock[ItemService[IO]]
        val controller      = new ItemController[IO](itemServiceMock)

        val testitem = item("item-1")
        when(itemServiceMock.findBy(any[Brand.Name])).thenReturn(fs2.Stream.emits(List(testitem)).lift[IO])

        val request  = Request[IO](uri = uri"/items?brand=test-brand")
        val response = controller.routes(adminMiddleware).orNotFound.run(request)

        val expectedResponse = ItemResponse(
          testitem.id,
          testitem.name,
          testitem.description,
          testitem.price,
          testitem.brand.name,
          testitem.category.name
        )
        verifyResponse[List[ItemResponse]](response, Status.Ok, Some(List(expectedResponse)))
        verify(itemServiceMock).findBy(Brand.Name("Test-brand"))
      }

      "return error when brand is blank" in {
        val itemServiceMock = mock[ItemService[IO]]
        val controller      = new ItemController[IO](itemServiceMock)

        val request  = Request[IO](uri = uri"/items?brand=")
        val response = controller.routes(adminMiddleware).orNotFound.run(request)

        verifyResponse[String](response, Status.BadRequest, Some("Brand must not be blank"))
        verify(itemServiceMock, never).findBy(any[Brand.Name])
        verify(itemServiceMock, never).findAll
      }
    }

    "PUT /admin/items/{id}" should {

      val updateRequest = json"""{"price": 14.99}"""

      "return no content on success" in {
        val itemServiceMock = mock[ItemService[IO]]
        val controller      = new ItemController[IO](itemServiceMock)

        when(itemServiceMock.update(any[UpdateItem])).thenReturn(IO.unit)

        val request = Request[IO](uri = uri"/admin/items/607995e0-8e3a-11ea-bc55-0242ac130003", method = Method.PUT)
          .withEntity(updateRequest)
        val response = controller.routes(adminMiddleware).orNotFound.run(request)

        verifyResponse[ItemResponse](response, Status.NoContent, None)
        verify(itemServiceMock).update(UpdateItem(Item.Id(itemId), GBP(14.99)))
      }

      "return 404 when item not found" in {
        val itemServiceMock = mock[ItemService[IO]]
        val controller      = new ItemController[IO](itemServiceMock)

        when(itemServiceMock.update(any[UpdateItem])).thenReturn(IO.raiseError(ItemNotFound(Item.Id(itemId))))

        val request =
          Request[IO](uri = uri"/admin/items/607995e0-8e3a-11ea-bc55-0242ac130003", method = Method.PUT).withEntity(updateRequest)
        val response = controller.routes(adminMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](
          response,
          Status.NotFound,
          Some(ErrorResponse("Item with id 607995e0-8e3a-11ea-bc55-0242ac130003 does not exist"))
        )
        verify(itemServiceMock).update(UpdateItem(Item.Id(itemId), GBP(14.99)))
      }
    }

    "POST /admin/items" should {

      val brandId    = UUID.fromString("2fd275de-99fb-11ea-bb37-0242ac130002")
      val categoryId = UUID.fromString("2fd277e6-99fb-11ea-bb37-0242ac130002")

      def createRequest(name: String = "test-item") =
        json"""{"name": $name, "description": "test-item description", "price": 10.99, "brandId": $brandId, "categoryId": $categoryId}"""

      "return no content on success" in {
        val itemServiceMock = mock[ItemService[IO]]
        val controller      = new ItemController[IO](itemServiceMock)

        when(itemServiceMock.create(any[CreateItem])).thenReturn(IO.pure(Item.Id(itemId)))

        val request  = Request[IO](uri = uri"/admin/items", method = Method.POST).withEntity(createRequest())
        val response = controller.routes(adminMiddleware).orNotFound.run(request)

        verifyResponse[ItemCreateResponse](response, Status.Created, Some(ItemCreateResponse(Item.Id(itemId))))
        verify(itemServiceMock).create(
          CreateItem(
            Item.Name("test-item"),
            Item.Description("test-item description"),
            GBP(10.99),
            Brand.Id(brandId),
            Category.Id(categoryId)
          )
        )
      }
    }
  }
}
