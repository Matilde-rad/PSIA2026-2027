-- Rows from before are NULL and are recomputed on the next run.
ALTER TABLE recommendation_cache ADD COLUMN fingerprint text;
