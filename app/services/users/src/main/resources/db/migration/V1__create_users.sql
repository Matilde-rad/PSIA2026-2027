CREATE TABLE users (
    id            uuid         PRIMARY KEY,
    username      varchar(32)  NOT NULL,
    password_hash varchar(60)  NOT NULL,
    bio           varchar(500),
    created_at    timestamptz  NOT NULL,
    CONSTRAINT users_username_key UNIQUE (username),
    CONSTRAINT users_username_lowercase CHECK (username = lower(username))
);

CREATE TABLE user_genres (
    user_id uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    genre   varchar(32) NOT NULL,
    PRIMARY KEY (user_id, genre)
);
