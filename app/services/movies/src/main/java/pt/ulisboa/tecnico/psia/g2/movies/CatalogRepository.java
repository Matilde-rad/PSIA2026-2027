package pt.ulisboa.tecnico.psia.g2.movies;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CatalogRepository {

    private static final String STORE_EMBEDDINGS_SQL = """
            UPDATE movie m SET embedding = v.embedding::vector
            FROM unnest(?::int[], ?::text[]) AS v(id, embedding)
            WHERE m.id = v.id
            """;

    private static final String DROP_INDEX_SQL = "DROP INDEX IF EXISTS movie_embedding_idx";

    private static final String CREATE_INDEX_SQL =
            "CREATE INDEX IF NOT EXISTS movie_embedding_idx ON movie USING hnsw (embedding vector_cosine_ops)";

    private static final String EMBEDDED_SQL = "SELECT count(*) FROM movie WHERE embedding IS NOT NULL";

    private static final String MISSING_SQL = "SELECT count(*) FROM movie WHERE embedding IS NULL";

    private static final String GENRES_SQL = "SELECT DISTINCT unnest(genres) AS genre FROM movie ORDER BY genre";

    private static final String BY_IDS_SQL =
            "SELECT id, title, year, genres, rating_count, imdb_id FROM movie WHERE id = ANY(?)";

    static final int MAX_SEARCH_WORDS = 6;

    private static final String SIMILAR_SQL = """
            SELECT m.id, m.title, m.year, m.genres, m.rating_count, m.imdb_id,
                   1 - (m.embedding <=> ?::vector) AS similarity
            FROM movie m
            WHERE m.embedding IS NOT NULL AND m.rating_count >= ?
            ORDER BY m.embedding <=> ?::vector
            LIMIT ?
            """;

    @Inject
    AgroalDataSource dataSource;

    @ConfigProperty(name = "movies.images.poster-url")
    String posterUrl;

    @ConfigProperty(name = "movies.images.banner-url")
    String bannerUrl;

    // Vectors already written as pgvector literals ("[0.1,-0.2,...]"), as in the precomputed CSV. One
    // statement per call, so the whole chunk is one round trip and one commit.
    public void storeVectorLiterals(Map<Integer, String> vectors) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(STORE_EMBEDDINGS_SQL)) {
            ps.setArray(1, c.createArrayOf("int4", vectors.keySet().toArray()));
            ps.setArray(2, c.createArrayOf("text", vectors.values().toArray()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not store embeddings", e);
        }
    }

    // A bulk load drops the HNSW index first: inserting the 87k vectors into it one by one took
    // 9.5 min, loading them without it about 15 s, and building it afterwards about 80 s.
    public void dropEmbeddingIndex() {
        execute(DROP_INDEX_SQL);
    }

    // No parallel workers: they build the graph in /dev/shm, which is 64 MB in the Postgres
    // container, and the build fails. The 87k vectors alone take about 360 MB of that memory.
    public void createEmbeddingIndex() {
        execute("SET LOCAL maintenance_work_mem = '1GB'", "SET LOCAL max_parallel_maintenance_workers = 0",
                CREATE_INDEX_SQL);
    }

    public int missingEmbeddings() {
        return count(MISSING_SQL);
    }

    public int embedded() {
        return count(EMBEDDED_SQL);
    }

    // every genre at least one movie has, in alphabetical order
    public List<String> genres() {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(GENRES_SQL);
                ResultSet rs = ps.executeQuery()) {
            List<String> genres = new ArrayList<>();
            while (rs.next()) {
                genres.add(rs.getString(1));
            }
            return genres;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read genres", e);
        }
    }

    // Title autocomplete. Every word typed must start a word of the title, in any order, so
    // "the matrix" finds "Matrix, The (1999)". Titles that begin with what was typed come first,
    // then the most rated. A leading article does not count on either side: "the matrix" and
    // "matrix" both put "Matrix, The (1999)" in that first group.
    public List<MovieSummary> search(String text, int limit) {
        List<String> words = words(text);
        if (words.isEmpty()) {
            return List.of();
        }
        StringBuilder sql = new StringBuilder(
                "SELECT id, title, year, genres, rating_count, imdb_id FROM movie WHERE true");
        words.forEach(w -> sql.append(" AND title ~* ?"));
        sql.append(" ORDER BY (regexp_replace(lower(title), '^(the|an|a) ', '') LIKE ?) DESC,"
                + " rating_count DESC, id LIMIT ?");
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1;
            for (String word : words) {
                ps.setString(i++, "\\m" + word);
            }
            ps.setString(i++, String.join(" ", withoutLeadingArticle(words)) + "%");
            ps.setInt(i, limit);
            return summaries(ps);
        } catch (SQLException e) {
            throw new IllegalStateException("Catalog query failed", e);
        }
    }

    // Letters and digits only, so a word can go into a regular expression as it is.
    static List<String> words(String text) {
        if (text == null) {
            return List.of();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(w -> !w.isEmpty())
                .limit(MAX_SEARCH_WORDS)
                .toList();
    }

    static List<String> withoutLeadingArticle(List<String> words) {
        boolean article = words.size() > 1 && List.of("the", "an", "a").contains(words.getFirst());
        return article ? words.subList(1, words.size()) : words;
    }

    // The movies with these ids, in the order asked; ids the catalog does not have are left out.
    public List<MovieSummary> byIds(List<Integer> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(BY_IDS_SQL)) {
            ps.setArray(1, c.createArrayOf("int4", ids.toArray()));
            Map<String, MovieSummary> found = new HashMap<>();
            summaries(ps).forEach(m -> found.put(m.id(), m));
            return ids.stream().distinct().map(id -> found.get(String.valueOf(id))).filter(Objects::nonNull).toList();
        } catch (SQLException e) {
            throw new IllegalStateException("Catalog query failed", e);
        }
    }

    private List<MovieSummary> summaries(PreparedStatement ps) throws SQLException {
        List<MovieSummary> rows = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String imdbId = rs.getString("imdb_id");
                rows.add(new MovieSummary(
                        String.valueOf(rs.getInt("id")),
                        rs.getString("title"),
                        rs.getObject("year", Integer.class),
                        Arrays.asList((String[]) rs.getArray("genres").getArray()),
                        rs.getInt("rating_count"),
                        imdbId == null ? null : posterUrl.formatted(imdbId),
                        imdbId == null ? null : bannerUrl.formatted(imdbId)));
            }
        }
        return rows;
    }

    // The HNSW index returns at most hnsw.ef_search rows (default 40), so it is raised to the limit for
    // this one transaction. With the rating filter, the iterative scan keeps walking the index until
    // enough movies pass it instead of returning only the few among the first ef_search.
    public List<SimilarMovie> similar(float[] embedding, int limit, int minRatings) {
        String q = vector(embedding);
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try (var st = c.createStatement()) {
                st.execute("SET LOCAL hnsw.ef_search = " + Math.max(40, limit));
                st.execute("SET LOCAL hnsw.iterative_scan = relaxed_order");
            }
            List<SimilarMovie> rows = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(SIMILAR_SQL)) {
                ps.setString(1, q);
                ps.setInt(2, minRatings);
                ps.setString(3, q);
                ps.setInt(4, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(similarMovie(rs));
                    }
                }
            }
            c.commit();
            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException("Catalog query failed", e);
        }
    }

    private int count(String sql) {
        try (Connection c = dataSource.getConnection(); var st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Catalog query failed", e);
        }
    }

    private void execute(String... statements) {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try (var st = c.createStatement()) {
                for (String sql : statements) {
                    st.execute(sql);
                }
            }
            c.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Catalog statement failed", e);
        }
    }

    private static String vector(float[] values) {
        StringBuilder s = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                s.append(',');
            }
            s.append(values[i]);
        }
        return s.append(']').toString();
    }

    private SimilarMovie similarMovie(ResultSet rs) throws SQLException {
        String imdbId = rs.getString("imdb_id");
        return new SimilarMovie(
                String.valueOf(rs.getInt("id")),
                rs.getString("title"),
                rs.getObject("year", Integer.class),
                Arrays.asList((String[]) rs.getArray("genres").getArray()),
                rs.getInt("rating_count"),
                imdbId == null ? null : posterUrl.formatted(imdbId),
                imdbId == null ? null : bannerUrl.formatted(imdbId),
                rs.getDouble("similarity"));
    }
}
