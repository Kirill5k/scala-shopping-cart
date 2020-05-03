package io.kirill.shoppingcart

import cats.effect.testing.scalatest.AsyncIOSpec
import org.mockito.ArgumentMatchersSugar
import org.mockito.scalatest.MockitoSugar
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers

trait CatsIOSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with MockitoSugar with ArgumentMatchersSugar {

}
