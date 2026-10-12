package pt.ulisboa.tecnico.psia.g2.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

// Account data only. Profile data (bio, favorite genres) lives in Profile.
@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected User() {
    }

    // new user
    public User(String username, String passwordHash) {
        // Postgres timestamptz keeps microseconds. Without truncating, the 201 body (nanoseconds)
        // and a later GET (rounded to micros) would show different createdAt values.
        this(UUID.randomUUID(), username, passwordHash, Instant.now().truncatedTo(ChronoUnit.MICROS));
    }

    public User(UUID id, String username, String passwordHash, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
