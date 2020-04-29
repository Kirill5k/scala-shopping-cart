package io.kirill.shoppingcart

import cats.effect.{IO, Resource}
import cats.effect.testing.scalatest.AsyncIOSpec
import com.dimafeng.testcontainers.{ForAllTestContainer, ForEachTestContainer, PostgreSQLContainer}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import skunk.Session
import natchez.Trace.Implicits.noop

trait PostgresRepositorySpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with ForEachTestContainer {

  override val container =
    PostgreSQLContainer(databaseName = "store", username = "scala", password = "scala").configure { c =>
      c.withInitScript("database.sql")
    }

  lazy val Array(host, port) =
    container.jdbcUrl.substring(18).split("/").head.split(":")

  implicit lazy val session: Resource[IO, Session[IO]] =
    Session.single[IO](host = host, port = port.toInt, user = "scala", password = Some("scala"), database = "store")
}
