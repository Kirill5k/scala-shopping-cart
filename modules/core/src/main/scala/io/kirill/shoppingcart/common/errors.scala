package io.kirill.shoppingcart.common

import io.kirill.shoppingcart.auth.user.User
import io.kirill.shoppingcart.shop.brand.Brand
import io.kirill.shoppingcart.shop.category.Category
import io.kirill.shoppingcart.shop.item.Item
import io.kirill.shoppingcart.shop.order.Order


object errors {
  sealed trait AppError extends Throwable {
    def message: String
    override def getMessage: String = message
  }

  sealed trait BadRequestError extends AppError
  sealed trait NotFoundError   extends AppError

  final case class ForeignKeyViolation(message: String) extends BadRequestError
  final case class UniqueViolation(message: String)     extends BadRequestError
  final case class ProcessingError(message: String)     extends BadRequestError

  final case class ItemNotFound(itemId: Item.Id) extends NotFoundError {
    val message = s"Item with id ${itemId.value} does not exist"
  }

  final case class OrderNotFound(orderId: Order.Id) extends NotFoundError {
    val message = s"Order with id ${orderId.value} does not exist"
  }

  final case class OrderDoesNotBelongToThisUser(orderId: Order.Id, userId: User.Id) extends AppError {
    val message = "Order does not belong to this user"
  }

  final case class UsernameInUse(username: User.Name) extends BadRequestError {
    val message = s"Username ${username.value} is already taken"
  }

  final case class BrandAlreadyExists(brandName: Brand.Name) extends BadRequestError {
    val message = s"Brand with name ${brandName.value} already exists"
  }

  final case class CategoryAlreadyExists(categoryName: Category.Name) extends BadRequestError {
    val message = s"Category with name ${categoryName.value} already exists"
  }

  final case object EmptyCart extends BadRequestError {
    val message = "Unable to checkout empty cart"
  }

  final case class InvalidUsernameOrPassword(username: User.Name) extends AppError {
    val message = "Username or password is incorrect"
  }

  final case class AuthTokenNotPresent(username: User.Name) extends AppError {
    val message = "Missing Authorization bearer token"
  }
}
