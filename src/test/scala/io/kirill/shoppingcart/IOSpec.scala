package io.kirill.shoppingcart

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.tests.CatsSuite
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

trait IOSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with ScalaCheckDrivenPropertyChecks with CatsSuite {

}
