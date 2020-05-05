package io.kirill.shoppingcart.common

import io.kirill.shoppingcart.auth.user.Username
import io.kirill.shoppingcart.shop.item.ItemId
import io.kirill.shoppingcart.shop.order.OrderId

object errors {
  sealed trait AppError extends Throwable {
    def message: String
    override def getMessage: String = message
  }

  sealed trait BadRequestError extends AppError
  sealed trait NotFoundError extends AppError

  final case class ForeignKeyViolation(message: String) extends BadRequestError
  final case class UniqueViolation(message: String) extends BadRequestError
  final case class ProcessingError(message: String) extends BadRequestError

  final case class ItemNotFound(itemId: ItemId) extends NotFoundError {
    val message = s"Item with id ${itemId.value} does not exist"
  }

  final case class OrderNotFound(orderId: OrderId) extends NotFoundError {
    val message = s"Order with id ${orderId.value} does not exist"
  }

  final case class UsernameInUse(username: Username) extends BadRequestError {
    val message = s"Username ${username.value} is already taken"
  }

  final case object EmptyCart extends BadRequestError {
    val message = "Unable to checkout empty cart"
  }

  final case object InvalidUsernameOrPassword extends AppError {
    val message = "Username or password is incorrect"
  }

  final case object AuthTokenNotPresent extends AppError {
    val message = "Missing Authorization bearer token"
  }

  final case object OrderDoesNotBelongToThisUser extends AppError {
    val message = "Order does not belong to this user"
  }
}
