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
import java.util.List;

@ApplicationScoped
public class CatalogRepository {

    @Inject
    AgroalDataSource dataSource;

    public List<String> genres() {
        String sql = "SELECT g FROM (SELECT DISTINCT unnest(genres) AS g FROM movie) t ORDER BY g COLLATE \"C\"";
        try (Connection c = dataSource.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
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

    public List<Candidate> candidates(List<String> genres, Integer yearFrom, Integer yearTo, int limit) {
        StringBuilder sql = new StringBuilder("SELECT id, title, year, genres, rating_count FROM movie WHERE true");
        if (!genres.isEmpty()) {
            sql.append(" AND genres @> ?");
        }
        if (yearFrom != null) {
            sql.append(" AND year >= ?");
        }
        if (yearTo != null) {
            sql.append(" AND year <= ?");
        }
        sql.append(" ORDER BY rating_count DESC, id LIMIT ?");
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1;
            if (!genres.isEmpty()) {
                ps.setArray(i++, c.createArrayOf("text", genres.toArray()));
            }
            if (yearFrom != null) {
                ps.setInt(i++, yearFrom);
            }
            if (yearTo != null) {
                ps.setInt(i++, yearTo);
            }
            ps.setInt(i, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Candidate> candidates = new ArrayList<>();
                while (rs.next()) {
                    String[] movieGenres = (String[]) rs.getArray("genres").getArray();
                    candidates.add(new Candidate(
                            String.valueOf(rs.getInt("id")),
                            rs.getString("title"),
                            rs.getObject("year", Integer.class),
                            Arrays.asList(movieGenres),
                            rs.getInt("rating_count")));
                }
                return candidates;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not query candidates", e);
        }
    }
}
