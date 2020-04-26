package io.kirill.shoppingcart.shop.item

import cats.effect.{ContextShift, IO}
import io.circe.Decoder
import io.circe.generic.auto._
import io.kirill.shoppingcart.ControllerSpec
import io.kirill.shoppingcart.shop.brand.BrandName
import io.kirill.shoppingcart.common.web.json._
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._

import scala.concurrent.ExecutionContext

class ItemControllerSpec extends ControllerSpec {
  import ItemBuilder._
  import ItemController._
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)

  "An ItemController" should {

    "return list of items on GET" in {
      val itemServiceMock = mock[ItemService[IO]]
      val controller      = new ItemController[IO](itemServiceMock)

      val items = List(item("item-1"), item("item-2"))
      when(itemServiceMock.findAll).thenReturn(IO.pure(items))

      val request = Request[IO](uri = uri"/items")
      val response: IO[Response[IO]] = controller.routes.orNotFound.run(request)

      verifyResponse[Seq[Item]](response, Status.Ok, Some(items))
      verify(itemServiceMock).findAll
    }

    "return list of items on GET by brand" in {
      val itemServiceMock = mock[ItemService[IO]]
      val controller      = new ItemController[IO](itemServiceMock)

      val items = List(item("item-1"), item("item-2"))
      when(itemServiceMock.findBy(any[BrandName])).thenReturn(IO.pure(items))

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