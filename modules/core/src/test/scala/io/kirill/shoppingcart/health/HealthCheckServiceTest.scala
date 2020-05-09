package io.kirill.shoppingcart.health

import io.kirill.shoppingcart.{PostgresRepositorySpec, RedisSpec}

class HealthCheckServiceTest extends RedisSpec with PostgresRepositorySpec {

  "A HealthCheckService" - {
    "return status of postgres and redis" in {
      withRedisAsync() { port =>
        val result = for {
          service <- HealthCheckService.make(session, stringCommands(port))
          health <- service.status
        } yield health

        result.asserting(_ must be (AppStatus(PostgresStatus(true), RedisStatus(true))))
      }
    }
  }
}
