package io.kirill.shoppingcart.shop

import java.util.UUID

import eu.timepit.refined._
import eu.timepit.refined.api._
import eu.timepit.refined.boolean.And
import eu.timepit.refined.collection.Size
import eu.timepit.refined.string.{MatchesRegex, ValidInt}
import io.kirill.shoppingcart.shop.order.Order

package object payment {
  type Rgx = W.`"^[a-zA-Z]+(([',. -][a-zA-Z ])?[a-zA-Z]*)*$"`.T

  type Name       = String Refined MatchesRegex[Rgx]
  type Number     = Long Refined Size[16]
  type Expiration = String Refined (Size[4] And ValidInt)
  type CVV        = Int Refined Size[3]

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
