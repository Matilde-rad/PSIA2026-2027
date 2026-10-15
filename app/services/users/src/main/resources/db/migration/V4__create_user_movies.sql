CREATE TABLE user_movies (
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    movie_id   varchar(9)  NOT NULL,
    status     varchar(8)  NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, movie_id),
    CONSTRAINT user_movies_status CHECK (status IN ('to_watch', 'liked', 'disliked'))
);

-- a user's list and their recent activity both read newest first
CREATE INDEX user_movies_recent_idx ON user_movies (user_id, updated_at DESC);
