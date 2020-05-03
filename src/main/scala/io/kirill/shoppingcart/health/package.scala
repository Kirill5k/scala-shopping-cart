package io.kirill.shoppingcart

package object health {

  final case class RedisStatus(value: Boolean)    extends AnyVal
  final case class PostgresStatus(value: Boolean) extends AnyVal

  final case class AppStatus(
      postgres: PostgresStatus,
      redis: RedisStatus
  )
}
