package io.kirill.shoppingcart.shop.brand

import cats.effect.IO
import io.circe._
import io.circe.generic.auto._
import io.circe.literal._
import io.kirill.shoppingcart.ControllerSpec
import io.kirill.shoppingcart.common.errors.BrandAlreadyExists
import io.kirill.shoppingcart.common.json._
import io.kirill.shoppingcart.common.web.ErrorResponse
import io.kirill.shoppingcart.shop.brand.BrandController.BrandCreateResponse
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._

import java.util.UUID

class BrandControllerSpec extends ControllerSpec {

  val brandId   = Brand.Id(UUID.fromString("d09c402a-8615-11ea-bc55-0242ac130003"))
  val testBrand = Brand(brandId, Brand.Name("test-brand"))

  "A BrandController" should {

    def brandCreateRequestJson(name: String = "test-brand"): Json =
      json"""{"name":$name}"""

    "GET /brands" should {
      "return all brands" in {
        val brandServiceMock = mock[BrandService[IO]]
        val controller       = new BrandController[IO](brandServiceMock)

        when(brandServiceMock.findAll).thenReturn(fs2.Stream.emits(List(testBrand)).lift[IO])

        val request                    = Request[IO](uri = uri"/brands", method = Method.GET)
        val response: IO[Response[IO]] = controller.routes(adminMiddleware).orNotFound.run(request)

        verifyResponse[List[Brand]](response, Status.Ok, Some(List(testBrand)))
        verify(brandServiceMock).findAll
      }
    }

    "POST /admin/brands" should {
      "create new brand when success" in {
        val brandServiceMock = mock[BrandService[IO]]
        val controller       = new BrandController[IO](brandServiceMock)

        when(brandServiceMock.create(any[Brand.Name])).thenReturn(IO.pure(brandId))

        val request                    = Request[IO](uri = uri"/admin/brands", method = Method.POST).withEntity(brandCreateRequestJson())
        val response: IO[Response[IO]] = controller.routes(adminMiddleware).orNotFound.run(request)

        verifyResponse[BrandCreateResponse](response, Status.Created, Some(BrandCreateResponse(brandId)))
        verify(brandServiceMock).create(Brand.Name("Test-brand"))
      }

      "return bad request when brand name is taken" in {
        val brandServiceMock = mock[BrandService[IO]]
        val controller       = new BrandController[IO](brandServiceMock)

        when(brandServiceMock.create(any[Brand.Name])).thenReturn(IO.raiseError(BrandAlreadyExists(testBrand.name)))

        val request                    = Request[IO](uri = uri"/admin/brands", method = Method.POST).withEntity(brandCreateRequestJson())
        val response: IO[Response[IO]] = controller.routes(adminMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](response, Status.BadRequest, Some(ErrorResponse("Brand with name test-brand already exists")))
        verify(brandServiceMock).create(Brand.Name("Test-brand"))
      }
    }
  }
}
