package io.kirill.shoppingcart

import io.kirill.shoppingcart.shop.brand.{Brand, BrandId, BrandName}
import io.kirill.shoppingcart.shop.cart.{Cart, CartItem, Quantity}
import io.kirill.shoppingcart.shop.category.{Category, CategoryId, CategoryName}
import io.kirill.shoppingcart.shop.item.{Item, ItemDescription, ItemId, ItemName}
import io.kirill.shoppingcart.shop.payment.{Card, CardCvv, CardExpiration, CardName, CardNumber}
import org.scalacheck.Gen
import squants.market.{Money, USD}

object generators {

  val genMoney: Gen[Money] =
    Gen.posNum[Long].map(n => USD(BigDecimal(n)))

  val genNonEmptyString: Gen[String] =
    Gen
      .chooseNum(21, 40)
      .flatMap { n =>
        Gen.buildableOfN[String, Char](n, Gen.alphaChar)
      }

  val brandGen: Gen[Brand] =
    for {
      i <- Gen.uuid.map(BrandId)
      n <- genNonEmptyString.map(BrandName)
    } yield Brand(i, n)

  val categoryGen: Gen[Category] =
    for {
      i <- Gen.uuid.map(CategoryId)
      n <- genNonEmptyString.map(CategoryName)
    } yield Category(i, n)

  val itemGen: Gen[Item] =
    for {
      i <- Gen.uuid.map(ItemId)
      n <- genNonEmptyString.map(ItemName)
      d <- genNonEmptyString.map(ItemDescription)
      p <- genMoney
      b <- brandGen
      c <- categoryGen
    } yield Item(i, n, d, p, b, c)

  val cartItemGen: Gen[CartItem] =
    for {
      i <- itemGen
      q <- Gen.posNum[Int].map(Quantity)
    } yield CartItem(i, q)

  val cartGen: Gen[Cart] =
    for {
      i <- Gen.nonEmptyListOf(cartItemGen)
      t <- genMoney
    } yield Cart(i, t)

  val cardGen: Gen[Card] =
    for {
      n <- genNonEmptyString.map(CardName)
      u <- Gen.posNum[Long].map(CardNumber)
      x <- Gen.posNum[Int].map(CardExpiration)
      c <- Gen.posNum[Int].map(CardCvv)
    } yield Card(n, u, x, c)
}
