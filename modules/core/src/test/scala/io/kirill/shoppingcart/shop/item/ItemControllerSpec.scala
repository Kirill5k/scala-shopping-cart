package io.kirill.shoppingcart.shop.item

import java.util.UUID

import cats.effect.{ContextShift, IO}
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.literal._
import io.kirill.shoppingcart.ControllerSpec
import io.kirill.shoppingcart.common.errors.ItemNotFound
import io.kirill.shoppingcart.shop.brand.{BrandId, BrandName}
import io.kirill.shoppingcart.common.json._
import io.kirill.shoppingcart.common.web.RestController.ErrorResponse
import io.kirill.shoppingcart.shop.category.CategoryId
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import squants.market.GBP

import scala.concurrent.ExecutionContext

class ItemControllerSpec extends ControllerSpec {
  import ItemBuilder._
  import ItemController._
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)

  val itemId = UUID.fromString("607995e0-8e3a-11ea-bc55-0242ac130003")

  "An ItemController" should {

    "GET /items/{id}" should {
      "find item by id" in {
        val itemServiceMock = mock[ItemService[IO]]
        val controller      = new ItemController[IO](itemServiceMock)

        val testitem = item("item-1").copy(id = ItemId(itemId))
        when(itemServiceMock.findById(any[ItemId])).thenReturn(IO.pure(testitem))

        val request = Request[IO](uri = uri"/items/607995e0-8e3a-11ea-bc55-0242ac130003")
        val response: IO[Response[IO]] = controller.routes(adminMiddleware).orNotFound.run(request)

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

        when(itemServiceMock.findById(any[ItemId])).thenReturn(IO.raiseError(ItemNotFound(ItemId(itemId))))

        val request = Request[IO](uri = uri"/items/607995e0-8e3a-11ea-bc55-0242ac130003")
        val response: IO[Response[IO]] = controller.routes(adminMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](response, Status.NotFound, Some(ErrorResponse("Item with id 607995e0-8e3a-11ea-bc55-0242ac130003 does not exist")))
        verify(itemServiceMock).findById(ItemId(itemId))
      }
    }

    "GET /items" should {
      "return list of items on GET" in {
        val itemServiceMock = mock[ItemService[IO]]
        val controller      = new ItemController[IO](itemServiceMock)

        val testitem = item("item-1")
        when(itemServiceMock.findAll).thenReturn(fs2.Stream.emits(List(testitem)).lift[IO])

        val request = Request[IO](uri = uri"/items")
        val response: IO[Response[IO]] = controller.routes(adminMiddleware).orNotFound.run(request)

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
        when(itemServiceMock.findBy(any[BrandName])).thenReturn(fs2.Stream.emits(List(testitem)).lift[IO])

        val request = Request[IO](uri = uri"/items?brand=test-brand")
        val response: IO[Response[IO]] = controller.routes(adminMiddleware).orNotFound.run(request)

        val expectedResponse = ItemResponse(
          testitem.id,
          testitem.name,
          testitem.description,
          testitem.price,
          testitem.brand.name,
          testitem.category.name
        )
        verifyResponse[List[ItemResponse]](response, Status.Ok, Some(List(expectedResponse)))
        verify(itemServiceMock).findBy(BrandName("Test-brand"))
      }

      "return error when brand is blank" in {
        val itemServiceMock = mock[ItemService[IO]]
        val controller      = new ItemController[IO](itemServiceMock)

        val request = Request[IO](uri = uri"/items?brand=")
        val response: IO[Response[IO]] = controller.routes(adminMiddleware).orNotFound.run(request)

        verifyResponse[String](response, Status.BadRequest, Some("Brand must not be blank"))
        verify(itemServiceMock, never).findBy(any[BrandName])
        verify(itemServiceMock, never).findAll
      }
    }

    "PUT /admin/items/{id}" should {

      def updateRequest(price: Double = 14.99) = json"""{"price": $price}"""

      "return no content on success" in {
        val itemServiceMock = mock[ItemService[IO]]
        val controller      = new ItemController[IO](itemServiceMock)

        when(itemServiceMock.update(any[UpdateItem])).thenReturn(IO.unit)

        val request = Request[IO](uri = uri"/admin/items/607995e0-8e3a-11ea-bc55-0242ac130003", method = Method.PUT).withEntity(updateRequest())
        val response: IO[Response[IO]] = controller.routes(adminMiddleware).orNotFound.run(request)

        verifyResponse[ItemResponse](response, Status.NoContent, None)
        verify(itemServiceMock).update(UpdateItem(ItemId(itemId), GBP(14.99)))
      }

      "return 404 when item not found" in {
        val itemServiceMock = mock[ItemService[IO]]
        val controller      = new ItemController[IO](itemServiceMock)

        when(itemServiceMock.update(any[UpdateItem])).thenReturn(IO.raiseError(ItemNotFound(ItemId(itemId))))

        val request = Request[IO](uri = uri"/admin/items/607995e0-8e3a-11ea-bc55-0242ac130003", method = Method.PUT).withEntity(updateRequest())
        val response: IO[Response[IO]] = controller.routes(adminMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](response, Status.NotFound, Some(ErrorResponse("Item with id 607995e0-8e3a-11ea-bc55-0242ac130003 does not exist")))
        verify(itemServiceMock).update(UpdateItem(ItemId(itemId), GBP(14.99)))
      }
    }

    "POST /admin/items" should {

      val brandId = UUID.fromString("2fd275de-99fb-11ea-bb37-0242ac130002")
      val categoryId = UUID.fromString("2fd277e6-99fb-11ea-bb37-0242ac130002")

      def createRequest(name: String = "test-item") =
        json"""{"name": $name, "description": "test-item description", "price": 10.99, "brandId": $brandId, "categoryId": $categoryId}"""

      "return no content on success" in {
        val itemServiceMock = mock[ItemService[IO]]
        val controller      = new ItemController[IO](itemServiceMock)

        when(itemServiceMock.create(any[CreateItem])).thenReturn(IO.pure(ItemId(itemId)))

        val request = Request[IO](uri = uri"/admin/items", method = Method.POST).withEntity(createRequest())
        val response: IO[Response[IO]] = controller.routes(adminMiddleware).orNotFound.run(request)

        verifyResponse[ItemCreateResponse](response, Status.Created, Some(ItemCreateResponse(ItemId(itemId))))
        verify(itemServiceMock).create(CreateItem(ItemName("test-item"), ItemDescription("test-item description"), GBP(10.99), BrandId(brandId), CategoryId(categoryId)))
      }
    }
  }
}
