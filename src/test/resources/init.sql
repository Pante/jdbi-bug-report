CREATE DATABASE sample;

CREATE TABLE foo (
    id uuid PRIMARY KEY,
    next uuid REFERENCES foo(id) DEFERRABLE INITIALLY DEFERRED,

    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL CHECK (updated_at >= created_at)
);
