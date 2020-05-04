package io.kirill.shoppingcart.common

import io.kirill.shoppingcart.auth.user.Username
import io.kirill.shoppingcart.shop.item.ItemId

object errors {
  sealed trait AppError extends Throwable {
    def message: String
    override def getMessage: String = message
  }

  final case class ForeignKeyViolation(message: String) extends AppError
  final case class UniqueViolation(message: String) extends AppError

  final case class ProcessingError(message: String) extends AppError

  final case class ItemNotFound(itemId: ItemId) extends AppError {
    val message = s"Item with id ${itemId.value} does not exist"
  }

  final case class UsernameInUse(username: Username) extends AppError {
    val message = s"Username ${username.value} is already taken"
  }

  case object InvalidUsernameOrPassword extends AppError {
    val message = "Username or password is incorrect"
  }

  final case object AuthTokenNotPresent extends AppError {
    val message = "Missing Authorization bearer token"
  }
}
