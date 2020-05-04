DROP DATABASE IF EXISTS shop;
CREATE DATABASE shop;

CREATE TABLE users
(
    id       UUID PRIMARY KEY,
    name     VARCHAR UNIQUE NOT NULL,
    password VARCHAR        NOT NULL
);

CREATE TABLE brands
(
    id   UUID PRIMARY KEY,
    name VARCHAR UNIQUE NOT NULL
);

CREATE TABLE categories
(
    id   UUID PRIMARY KEY,
    name VARCHAR UNIQUE NOT NULL
);

CREATE TABLE items
(
    id          UUID PRIMARY KEY,
    name        VARCHAR UNIQUE NOT NULL,
    description VARCHAR        NOT NULL,
    price       NUMERIC        NOT NULL,
    brand_id    UUID           NOT NULL,
    category_id UUID           NOT NULL,
    CONSTRAINT brand_id_fkey FOREIGN KEY (brand_id)
        REFERENCES brands (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT category_id_fkey FOREIGN KEY (category_id)
        REFERENCES categories (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE orders
(
    id          UUID PRIMARY KEY,
    status      VARCHAR     NOT NULL,
    user_id     UUID        NOT NULL,
    payment_id  UUID UNIQUE NOT NULL,
    items       JSONB       NOT NULL,
    total_price NUMERIC     NOT NULL,
    CONSTRAINT user_id_fkey FOREIGN KEY (user_id)
        REFERENCES users (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION
);

