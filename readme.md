# Scala-shopping-cart

A basic backend system for imaginary online store.

## Technical stack
- cats and cats-effect: basic functional blocks as well as concurrency and functional effects
- circe: JSON serialization library
- pureconfig: configuration parsing library
- http4s: functional HTTP server and client built on top of fs2
- http4s-jwt-auth: JWT authentication for http4s
- log4cats: standard logging framework for Cats
- redis4cats: client for Redis compatible with cats-effect
- refined: refinement types for type-level validation
- skunk: functional non-blocking PostgresSQL client

## Functionality

A guest user can:
- register into the system
- login with valid credentials
- see items from the catalogue as well as brands and categories

A registered user can:
- update content of the shopping cart
- place a new order by checking out the shopping cart
- view existing orders

An admin user can:
- add brands, categories and items
- modify items
