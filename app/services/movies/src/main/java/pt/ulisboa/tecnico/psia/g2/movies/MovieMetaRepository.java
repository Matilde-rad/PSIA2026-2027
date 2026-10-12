package pt.ulisboa.tecnico.psia.g2.movies;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Optional;

// The Cinemeta data already fetched, so a movie page asks Cinemeta only the first time.
@ApplicationScoped
public class MovieMetaRepository {

    private static final String FIND_SQL =
            "SELECT description, runtime_minutes, actors, directors, imdb_rating FROM movie_meta WHERE movie_id = ?";

    // two first visits at once both fetch; the second insert is simply dropped
    private static final String STORE_SQL = """
            INSERT INTO movie_meta (movie_id, description, runtime_minutes, actors, directors, imdb_rating)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (movie_id) DO NOTHING
            """;

    @Inject
    AgroalDataSource dataSource;

    public Optional<MovieMeta> find(int movieId) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(FIND_SQL)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                BigDecimal rating = rs.getBigDecimal("imdb_rating");
                return Optional.of(new MovieMeta(
                        rs.getString("description"),
                        rs.getObject("runtime_minutes", Integer.class),
                        Arrays.asList((String[]) rs.getArray("actors").getArray()),
                        Arrays.asList((String[]) rs.getArray("directors").getArray()),
                        rating == null ? null : rating.doubleValue()));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read movie meta", e);
        }
    }

    public void store(int movieId, MovieMeta meta) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(STORE_SQL)) {
            ps.setInt(1, movieId);
            ps.setString(2, meta.description());
            ps.setObject(3, meta.runtimeMinutes(), Types.INTEGER);
            ps.setArray(4, c.createArrayOf("text", meta.cast().toArray()));
            ps.setArray(5, c.createArrayOf("text", meta.directors().toArray()));
            ps.setBigDecimal(6, meta.imdbRating() == null ? null : BigDecimal.valueOf(meta.imdbRating()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not store movie meta", e);
        }
    }
}
