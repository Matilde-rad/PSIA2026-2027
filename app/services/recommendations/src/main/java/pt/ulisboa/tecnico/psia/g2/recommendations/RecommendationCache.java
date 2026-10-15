package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RecommendationCache {

    private static final String READ = "SELECT items FROM recommendation_cache WHERE user_id = ?";
    private static final String STAMP = "SELECT fingerprint, updated_at FROM recommendation_cache WHERE user_id = ?";
    private static final String REPLACE = "INSERT INTO recommendation_cache (user_id, items, fingerprint, updated_at) VALUES (?, CAST(? AS jsonb), ?, now()) ON CONFLICT (user_id) DO UPDATE SET items = EXCLUDED.items, fingerprint = EXCLUDED.fingerprint, updated_at = EXCLUDED.updated_at";

    public record Stamp(String fingerprint, Instant updatedAt) {
    }

    @Inject
    AgroalDataSource dataSource;

    @Inject
    ObjectMapper mapper;

    public List<Recommendation> read(String userId) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(READ)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return List.of();
                }
                return mapper.readValue(rs.getString(1), new TypeReference<>() {
                });
            }
        } catch (SQLException | JsonProcessingException e) {
            throw new IllegalStateException("Could not read cached recommendations", e);
        }
    }

    public Stamp stamp(String userId) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(STAMP)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Stamp(rs.getString(1), rs.getTimestamp(2).toInstant()) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read cached recommendations", e);
        }
    }

    public void replace(String userId, List<Recommendation> pool, String fingerprint) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(REPLACE)) {
            ps.setString(1, userId);
            ps.setString(2, mapper.writeValueAsString(pool));
            ps.setString(3, fingerprint);
            ps.executeUpdate();
        } catch (SQLException | JsonProcessingException e) {
            throw new IllegalStateException("Could not write cached recommendations", e);
        }
    }
}
