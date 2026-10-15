CREATE TABLE movie_meta (
    movie_id        integer PRIMARY KEY REFERENCES movie (id) ON DELETE CASCADE,
    description     text,
    runtime_minutes integer,
    actors          text[]  NOT NULL,
    directors       text[]  NOT NULL,
    imdb_rating     numeric(3, 1),
    fetched_at      timestamptz NOT NULL DEFAULT now()
);
