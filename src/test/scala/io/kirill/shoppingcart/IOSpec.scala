package io.kirill.shoppingcart

import java.util.UUID

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.tests.CatsSuite
import io.kirill.shoppingcart.brand.{Brand, BrandId, BrandName}
import org.http4s.CharsetRange.*
import org.scalacheck.Gen
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

trait IOSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with ScalaCheckDrivenPropertyChecks with CatsSuite {

}
