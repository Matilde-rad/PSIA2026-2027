CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE movie (
    id           integer PRIMARY KEY,
    title        text    NOT NULL,
    year         integer,
    genres       text[]  NOT NULL,
    rating_count integer NOT NULL DEFAULT 0,
    imdb_id      text,
    embedding    vector(1024)
);

CREATE INDEX movie_embedding_idx ON movie USING hnsw (embedding vector_cosine_ops);
