package io.kirill.shoppingcart

import java.sql.{Connection, DriverManager}

import cats.effect.{IO, Resource}
import cats.effect.testing.scalatest.AsyncIOSpec
import natchez.Trace
import org.scalactic.source.Position
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres
import ru.yandex.qatools.embed.postgresql.distribution.Version
import skunk.Session
import natchez.Trace.Implicits.noop

trait PostgresRepositorySpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with BeforeAndAfterAll with BeforeAndAfter {

  val postgres: EmbeddedPostgres = new EmbeddedPostgres(Version.V9_6_11)
  val url: String = postgres.start("localhost", 5432, "dbName", "userName", "password")
  val conn: Connection = DriverManager.getConnection(url)

  implicit val session: Resource[IO, Session[IO]] =
    Session.single[IO](host = "localhost", port = 5432, user = "postgres", database = "store")

  override protected def afterAll(): Unit = {
    conn.close
    postgres.stop()
  }
}
