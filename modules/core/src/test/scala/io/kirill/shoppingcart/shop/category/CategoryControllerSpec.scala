package io.kirill.shoppingcart.shop.category

import cats.effect.IO
import io.circe._
import io.circe.generic.auto._
import io.circe.literal._
import io.kirill.shoppingcart.ControllerSpec
import io.kirill.shoppingcart.common.errors.CategoryAlreadyExists
import io.kirill.shoppingcart.common.web.json._
import io.kirill.shoppingcart.common.web.ErrorResponse
import io.kirill.shoppingcart.shop.category.CategoryController.CategoryCreateResponse
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._

import java.util.UUID

class CategoryControllerSpec extends ControllerSpec {

  val categoryId   = Category.Id(UUID.fromString("d09c402a-8615-11ea-bc55-0242ac130003"))
  val testCategory = Category(categoryId, Category.Name("test-category"))

  "A CategoryController" should {

    def categoryCreateRequestJson(name: String = "test-category"): Json =
      json"""{"name":$name}"""

    "GET /categories" should {
      "return all categories" in {
        val categoryServiceMock = mock[CategoryService[IO]]
        val controller          = new CategoryController[IO](categoryServiceMock)

        when(categoryServiceMock.findAll).thenReturn(fs2.Stream.emits(List(testCategory)).lift[IO])

        val request  = Request[IO](uri = uri"/categories", method = Method.GET)
        val response = controller.routes(adminMiddleware).orNotFound.run(request)

        verifyResponse[List[Category]](response, Status.Ok, Some(List(testCategory)))
        verify(categoryServiceMock).findAll
      }
    }

    "POST /admin/categories" should {
      "create new category when success" in {
        val categoryServiceMock = mock[CategoryService[IO]]
        val controller          = new CategoryController[IO](categoryServiceMock)

        when(categoryServiceMock.create(any[Category.Name])).thenReturn(IO.pure(categoryId))

        val request  = Request[IO](uri = uri"/admin/categories", method = Method.POST).withEntity(categoryCreateRequestJson())
        val response = controller.routes(adminMiddleware).orNotFound.run(request)

        verifyResponse[CategoryCreateResponse](response, Status.Created, Some(CategoryCreateResponse(categoryId)))
        verify(categoryServiceMock).create(Category.Name("Test-category"))
      }

      "return bad request when category name is taken" in {
        val categoryServiceMock = mock[CategoryService[IO]]
        val controller          = new CategoryController[IO](categoryServiceMock)

        when(categoryServiceMock.create(any[Category.Name])).thenReturn(IO.raiseError(CategoryAlreadyExists(testCategory.name)))

        val request  = Request[IO](uri = uri"/admin/categories", method = Method.POST).withEntity(categoryCreateRequestJson())
        val response = controller.routes(adminMiddleware).orNotFound.run(request)

        verifyResponse[ErrorResponse](response, Status.BadRequest, Some(ErrorResponse("Category with name test-category already exists")))
        verify(categoryServiceMock).create(Category.Name("Test-category"))
      }
    }
  }
}
