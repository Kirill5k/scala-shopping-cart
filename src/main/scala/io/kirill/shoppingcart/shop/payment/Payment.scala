package io.kirill.shoppingcart.shop.payment

import java.util.UUID

import io.kirill.shoppingcart.auth.UserId
import squants.Money

final case class CardName(value: String)       extends AnyVal
final case class CardNumber(value: Long)       extends AnyVal
final case class CardExpiration(value: String) extends AnyVal
final case class CardCvv(value: Int)           extends AnyVal

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
