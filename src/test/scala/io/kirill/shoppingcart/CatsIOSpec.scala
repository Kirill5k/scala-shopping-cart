package io.kirill.shoppingcart

import cats.effect.testing.scalatest.AsyncIOSpec
import org.mockito.{ArgumentMatchersSugar, Strictness}
import org.mockito.scalatest.{AsyncMockitoSugar, MockitoSugar, ResetMocksAfterEachAsyncTest, ResetMocksAfterEachTest}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers

trait CatsIOSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with AsyncMockitoSugar with ResetMocksAfterEachAsyncTest {

}
