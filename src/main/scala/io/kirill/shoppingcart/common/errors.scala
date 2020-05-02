package io.kirill.shoppingcart.common

import io.kirill.shoppingcart.auth.user.Username

object errors {
  sealed trait AppError extends Throwable {
    def message: String
    override def getMessage: String = message
  }

  final case class ForeignKeyViolation(message: String) extends AppError
  final case class UniqueViolation(message: String) extends AppError

  final case class ItemNotFound(message: String) extends AppError
  final case class ProcessingError(message: String) extends AppError

  final case class UsernameInUse(username: Username) extends AppError {
    val message = s"Username ${username.value} is already taken"
  }

  final case object InvalidUsernameOrPassword extends AppError {
    val message = "Username or password is incorrect"
  }

  final case object AuthTokenNotPresent extends AppError {
    val message = "Missing Authorization bearer token"
  }
}
