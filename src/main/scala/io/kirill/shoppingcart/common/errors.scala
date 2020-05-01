package io.kirill.shoppingcart.common

object errors {
  sealed trait AppError extends Throwable {
    def message: String
    override def getMessage: String = message
  }

  final case class ForeignKeyViolation(message: String) extends AppError
  final case class UniqueViolation(message: String) extends AppError

  final case class ItemNotFound(message: String) extends AppError
  final case class ProcessingError(message: String) extends AppError

  final case object InvalidUsernameOrPassword extends AppError {
    val message = "Username or password is incorrect"
  }

  final case object AuthTokenNotPresent extends AppError {
    val message = "Missing Authorization bearer token"
  }
}
