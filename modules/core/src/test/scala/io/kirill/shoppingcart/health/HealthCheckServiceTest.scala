package io.kirill.shoppingcart.health

import io.kirill.shoppingcart.{PostgresRepositorySpec, RedisSpec}

class HealthCheckServiceTest extends RedisSpec with PostgresRepositorySpec {

  "A HealthCheckService" - {
    "return status of postgres and redis" in {
      withRedisCommands { redis =>
        val result = for {
          service <- HealthCheckService.make(session, redis)
          health  <- service.status
        } yield health

        result.asserting(_ mustBe AppStatus(AppStatus.Service(true), AppStatus.Service(true)))
      }
    }
  }
}
