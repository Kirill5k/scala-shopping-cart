package io.kirill.shoppingcart.shop.order

import cats.effect.{ContextShift, IO}
import io.kirill.shoppingcart.ControllerSpec

import scala.concurrent.ExecutionContext

class OrderControllerSpec extends ControllerSpec {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)

  "An OrderController" should {

    "checkout" should {

    }
  }
}
