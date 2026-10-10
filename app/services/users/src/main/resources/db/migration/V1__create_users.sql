CREATE TABLE users (
    id            uuid         PRIMARY KEY,
    username      varchar(32)  NOT NULL,
    password_hash varchar(60)  NOT NULL,
    created_at    timestamptz  NOT NULL,
    CONSTRAINT users_username_key UNIQUE (username),
    CONSTRAINT users_username_lowercase CHECK (username = lower(username))
);
