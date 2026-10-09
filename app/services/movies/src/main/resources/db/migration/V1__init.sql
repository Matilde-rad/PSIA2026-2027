CREATE TABLE movie (
    id           integer PRIMARY KEY,
    title        text    NOT NULL,
    year         integer,
    genres       text[]  NOT NULL,
    rating_count integer NOT NULL DEFAULT 0
);
