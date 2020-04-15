package io.kirill.shoppingcart.item

import cats.effect.{ContextShift, IO}
import io.circe.Decoder
import io.circe.generic.auto._
import io.kirill.shoppingcart.brand.BrandName
import io.kirill.shoppingcart.common.json._
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import org.mockito.ArgumentMatchersSugar
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import squants.market.{GBP, Money}

import scala.concurrent.ExecutionContext

class ItemControllerSpec extends AnyWordSpec with MockitoSugar with ArgumentMatchersSugar with Matchers {
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

  def verifyResponse[A](actual: IO[Response[IO]], expectedStatus: Status, expectedBody: Option[A] = None)(
      implicit ev: EntityDecoder[IO, A]
  ): Unit = {
    val actualResp = actual.unsafeRunSync

    actualResp.status must be(expectedStatus)
    expectedBody match {
      case Some(expected) => actualResp.as[A].unsafeRunSync must be(expected)
      case None           => actualResp.body.compile.toVector.unsafeRunSync mustBe empty
    }
  }
}
