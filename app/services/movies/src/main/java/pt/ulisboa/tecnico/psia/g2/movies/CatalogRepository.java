package pt.ulisboa.tecnico.psia.g2.movies;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CatalogRepository {

    private static final String GENRES_SQL = """
            SELECT genre
            FROM (SELECT DISTINCT unnest(genres) AS genre FROM movie) AS g
            ORDER BY genre COLLATE "C"
            """;

    private static final String CANDIDATES_SQL = """
            WITH filter AS (SELECT ?::text[] AS genres, ?::int AS year_from, ?::int AS year_to)
            SELECT m.id, m.title, m.year, m.genres, m.rating_count, m.imdb_id
            FROM movie m, filter f
            WHERE m.genres @> f.genres
              AND (f.year_from IS NULL OR m.year >= f.year_from)
              AND (f.year_to IS NULL OR m.year <= f.year_to)
            ORDER BY m.rating_count DESC, m.id
            LIMIT ?
            """;

    private static final String LOOKUP_SQL = """
            SELECT DISTINCT ON (k.position) m.id, m.title, m.year, m.genres, m.rating_count, m.imdb_id
            FROM unnest(?::text[]) WITH ORDINALITY AS k(title_key, position)
            JOIN movie m ON m.title_key = k.title_key
            ORDER BY k.position, m.rating_count DESC, m.id
            """;

    @Inject
    AgroalDataSource dataSource;

    @ConfigProperty(name = "movies.images.poster-url")
    String posterUrl;

    @ConfigProperty(name = "movies.images.banner-url")
    String bannerUrl;

    public List<String> genres() {
        return query(GENRES_SQL, ps -> {
        }, rs -> rs.getString("genre"));
    }

    public List<Candidate> candidates(List<String> genres, Integer yearFrom, Integer yearTo, int limit) {
        return query(CANDIDATES_SQL, ps -> {
            ps.setArray(1, textArray(ps, genres));
            ps.setObject(2, yearFrom, Types.INTEGER);
            ps.setObject(3, yearTo, Types.INTEGER);
            ps.setInt(4, limit);
        }, this::candidate);
    }

    public List<Candidate> lookup(List<String> titleKeys) {
        return query(LOOKUP_SQL, ps -> ps.setArray(1, textArray(ps, titleKeys)), this::candidate);
    }

    private <T> List<T> query(String sql, Binder binder, RowMapper<T> mapper) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Catalog query failed", e);
        }
    }

    private static java.sql.Array textArray(PreparedStatement ps, List<String> values) throws SQLException {
        return ps.getConnection().createArrayOf("text", values.toArray());
    }

    private Candidate candidate(ResultSet rs) throws SQLException {
        String imdbId = rs.getString("imdb_id");
        return new Candidate(
                String.valueOf(rs.getInt("id")),
                rs.getString("title"),
                rs.getObject("year", Integer.class),
                Arrays.asList((String[]) rs.getArray("genres").getArray()),
                rs.getInt("rating_count"),
                imdbId == null ? null : posterUrl.formatted(imdbId),
                imdbId == null ? null : bannerUrl.formatted(imdbId));
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}
