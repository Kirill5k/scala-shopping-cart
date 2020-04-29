DROP DATABASE IF EXISTS shop;
CREATE DATABASE shop;

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

