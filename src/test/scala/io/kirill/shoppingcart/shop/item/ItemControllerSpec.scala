package io.kirill.shoppingcart.shop.item

import java.util.UUID

import cats.effect.{ContextShift, IO}
import io.circe.Decoder
import io.circe.generic.auto._
import io.kirill.shoppingcart.ControllerSpec
import io.kirill.shoppingcart.common.errors.ItemNotFound
import io.kirill.shoppingcart.shop.brand.BrandName
import io.kirill.shoppingcart.common.json._
import io.kirill.shoppingcart.common.web.RestController.ErrorResponse
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._

import scala.concurrent.ExecutionContext

class ItemControllerSpec extends ControllerSpec {
  import ItemBuilder._
  import ItemController._
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)

  val itemId = UUID.fromString("607995e0-8e3a-11ea-bc55-0242ac130003")

  "An ItemController" should {

    "get/{id}" should {
      "find item by id" in {
        val itemServiceMock = mock[ItemService[IO]]
        val controller      = new ItemController[IO](itemServiceMock)

        val testitem = item("item-1").copy(id = ItemId(itemId))
        when(itemServiceMock.findById(any[ItemId])).thenReturn(IO.pure(testitem))

        val request = Request[IO](uri = uri"/items/607995e0-8e3a-11ea-bc55-0242ac130003")
        val response: IO[Response[IO]] = controller.routes.orNotFound.run(request)

        verifyResponse[Item](response, Status.Ok, Some(testitem))
        verify(itemServiceMock).findById(testitem.id)
      }

      "return 404 if item does not exist" in {
        val itemServiceMock = mock[ItemService[IO]]
        val controller      = new ItemController[IO](itemServiceMock)

        when(itemServiceMock.findById(any[ItemId])).thenReturn(IO.raiseError(ItemNotFound(ItemId(itemId))))

        val request = Request[IO](uri = uri"/items/607995e0-8e3a-11ea-bc55-0242ac130003")
        val response: IO[Response[IO]] = controller.routes.orNotFound.run(request)

        verifyResponse[ErrorResponse](response, Status.NotFound, Some(ErrorResponse("Item with id 607995e0-8e3a-11ea-bc55-0242ac130003 does not exist")))
        verify(itemServiceMock).findById(ItemId(itemId))
      }
    }

    "return list of items on GET" in {
      val itemServiceMock = mock[ItemService[IO]]
      val controller      = new ItemController[IO](itemServiceMock)

      val items = List(item("item-1"), item("item-2"))
      when(itemServiceMock.findAll).thenReturn(fs2.Stream.emits(items).lift[IO])

      val request = Request[IO](uri = uri"/items")
      val response: IO[Response[IO]] = controller.routes.orNotFound.run(request)

      verifyResponse[Seq[Item]](response, Status.Ok, Some(items))
      verify(itemServiceMock).findAll
    }

    "return list of items on GET by brand" in {
      val itemServiceMock = mock[ItemService[IO]]
      val controller      = new ItemController[IO](itemServiceMock)

      val items = List(item("item-1"), item("item-2"))
      when(itemServiceMock.findBy(any[BrandName])).thenReturn(fs2.Stream.emits(items).lift[IO])

      val request = Request[IO](uri = uri"/items?brand=test-brand")
      val response: IO[Response[IO]] = controller.routes.orNotFound.run(request)

      verifyResponse[Seq[Item]](response, Status.Ok, Some(items))
      verify(itemServiceMock).findBy(BrandName("Test-brand"))
    }

    "return error when brand is blank" in {
      val itemServiceMock = mock[ItemService[IO]]
      val controller      = new ItemController[IO](itemServiceMock)

      val request = Request[IO](uri = uri"/items?brand=")
      val response: IO[Response[IO]] = controller.routes.orNotFound.run(request)

      verifyResponse[String](response, Status.BadRequest, Some("Brand must not be blank"))
      verify(itemServiceMock, never).findBy(any[BrandName])
      verify(itemServiceMock, never).findAll
    }
  }
}
