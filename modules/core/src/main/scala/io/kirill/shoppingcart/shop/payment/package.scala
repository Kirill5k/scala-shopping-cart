package io.kirill.shoppingcart.shop

import java.util.UUID

import eu.timepit.refined._
import eu.timepit.refined.api._
import eu.timepit.refined.boolean.And
import eu.timepit.refined.numeric.{LessEqual, Positive}
import eu.timepit.refined.string.{MatchesRegex}
import io.kirill.shoppingcart.shop.order.Order

package object payment {
  type NameRegex       = W.`"^[a-zA-Z]+(([',. -][a-zA-Z ])?[a-zA-Z]*)*$"`.T
  type ExpirationRegex = W.`"^[0-9]{4}$"`.T

  type Name       = String Refined MatchesRegex[NameRegex]
  type Number     = Long Refined (Positive And LessEqual[9999999999999999L])
  type Expiration = String Refined MatchesRegex[ExpirationRegex]
  type CVV        = Int Refined (Positive And LessEqual[999])

  final case class Card(
      name: Name,
      number: Number,
      expiration: Expiration,
      cvv: CVV
  )

  final case class PaymentId(value: UUID) extends AnyVal

  final case class Payment(
      order: Order,
      card: Card
  )
}
