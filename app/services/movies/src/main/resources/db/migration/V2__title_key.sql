TRUNCATE movie;

ALTER TABLE movie ADD COLUMN title_key text NOT NULL;

CREATE INDEX movie_title_key_idx ON movie (title_key);
