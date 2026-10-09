CREATE TABLE recommendation_cache (
    user_id    text        PRIMARY KEY,
    items      jsonb       NOT NULL,
    updated_at timestamptz NOT NULL
);
