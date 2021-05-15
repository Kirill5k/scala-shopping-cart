package io.kirill.shoppingcart

import eu.timepit.refined.api.Refined
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import io.kirill.shoppingcart.shop.brand.Brand
import io.kirill.shoppingcart.shop.cart.{Cart, CartItem}
import io.kirill.shoppingcart.shop.category.Category
import io.kirill.shoppingcart.shop.item.Item
import io.kirill.shoppingcart.shop.payment.{Card, Payment}
import org.scalacheck.{Arbitrary, Gen}
import squants.market.{GBP, Money}

import java.util.UUID

object Generators {

  implicit def arbCoercibleUUID[A: Coercible[UUID, *]]: Arbitrary[A] = Arbitrary(cbUuid[A])

  def cbUuid[A: Coercible[UUID, *]]: Gen[A]  = Gen.uuid.map(_.coerce[A])
  def cbStr[A: Coercible[String, *]]: Gen[A] = nonEmptyStringGen.map(_.coerce[A])
  def cbInt[A: Coercible[Int, *]]: Gen[A]    = Gen.posNum[Int].map(_.coerce[A])

  val nonEmptyStringGen: Gen[String] =
    Gen.chooseNum(10, 30).flatMap(i => Gen.buildableOfN[String, Char](i, Gen.alphaChar))

  val moneyGen: Gen[Money] = Gen.choose[Double](1, 1000).map(d => GBP(BigDecimal(d)))

  val brandGen: Gen[Brand] =
    for {
      i <- cbUuid[Brand.Id]
      n <- cbStr[Brand.Name]
    } yield Brand(i, n)

  val categoryGen: Gen[Category] =
    for {
      i <- cbUuid[Category.Id]
      n <- cbStr[Category.Name]
    } yield Category(i, n)

  val itemGen: Gen[Item] =
    for {
      i <- cbUuid[Item.Id]
      n <- cbStr[Item.Name]
      d <- cbStr[Item.Description]
      p <- moneyGen
      b <- brandGen
      c <- categoryGen
    } yield Item(i, n, d, p, b, c)

  val cartItemGen: Gen[CartItem] =
    for {
      i <- cbUuid[Item.Id]
      q <- cbInt[Item.Quantity]
    } yield CartItem(i, q)

  val cartGen: Gen[Cart] =
    Gen.nonEmptyListOf(cartItemGen).map(i => Cart(i))

  val cardGen: Gen[Card] =
    for {
      n <- nonEmptyStringGen.map[Card.Name](Refined.unsafeApply)
      u <- Gen.posNum[Long].map[Card.Number](Refined.unsafeApply)
      x <- Gen.posNum[Int].map[Card.Expiration](x => Refined.unsafeApply(x.toString))
      c <- Gen.posNum[Int].map[Card.CVV](Refined.unsafeApply)
    } yield Card(n, u, x, c)
}
