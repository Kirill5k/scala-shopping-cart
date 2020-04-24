package io.kirill.shoppingcart.shop

import java.util.UUID

import eu.timepit.refined._
import eu.timepit.refined.api._
import eu.timepit.refined.boolean.And
import eu.timepit.refined.collection.Size
import eu.timepit.refined.string.{ MatchesRegex, ValidInt }
import io.kirill.shoppingcart.auth.UserId
import squants.Money

package object payment {
  type Rgx = W.`"^[a-zA-Z]+(([',. -][a-zA-Z ])?[a-zA-Z]*)*$"`.T

  type CardNamePred       = String Refined MatchesRegex[Rgx]
  type CardNumberPred     = Long Refined Size[16]
  type CardExpirationPred = String Refined (Size[4] And ValidInt)
  type CardCVVPred        = Int Refined Size[3]

  final case class CardName(value: String)    extends AnyVal
  final case class CardNumber(value: Long)    extends AnyVal
  final case class CardExpiration(value: Int) extends AnyVal
  final case class CardCvv(value: Int)        extends AnyVal

  final case class Card(
      name: CardName,
      number: CardNumber,
      expiration: CardExpiration,
      cvv: CardCvv
  )

  final case class PaymentId(value: UUID) extends AnyVal

  final case class Payment(
      id: UserId,
      total: Money,
      card: Card
  )
}
