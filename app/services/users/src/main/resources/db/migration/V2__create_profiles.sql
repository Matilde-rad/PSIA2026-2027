CREATE TABLE profiles (
    user_id            uuid          PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    display_name       varchar(64),
    bio                varchar(500),
    favorite_genres    jsonb         NOT NULL DEFAULT '[]',
    favorite_directors jsonb         NOT NULL DEFAULT '[]',
    favorite_movies    jsonb         NOT NULL DEFAULT '[]'
);
