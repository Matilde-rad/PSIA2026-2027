-- Every row has a vector, so EmbeddingLoader does not read the 1.2 GB embeddings file during tests.
INSERT INTO movie (id, title, year, genres, rating_count, imdb_id, embedding) VALUES
    (1, 'Matrix, The (1999)', 1999, '{Action,Sci-Fi,Thriller}', 500, NULL, array_fill(1, ARRAY[1024])::vector),
    (2, 'Matrix Reloaded, The (2003)', 2003, '{Action,Adventure,Sci-Fi,Thriller,IMAX}', 300, NULL, array_fill(1, ARRAY[1024])::vector),
    (3, 'Return to the Matrix (2010)', 2010, '{Comedy,Sci-Fi}', 900, NULL, array_fill(1, ARRAY[1024])::vector),
    (4, 'Spaceballs (1987)', 1987, '{Comedy,Sci-Fi}', 400, NULL, array_fill(1, ARRAY[1024])::vector),
    (5, 'Galaxy Quest (1999)', 1999, '{Adventure,Comedy,Sci-Fi}', 400, NULL, array_fill(1, ARRAY[1024])::vector),
    (6, 'Airplane! (1980)', 1980, '{Comedy}', 800, NULL, array_fill(1, ARRAY[1024])::vector);
