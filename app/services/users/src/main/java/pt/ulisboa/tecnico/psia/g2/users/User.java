package pt.ulisboa.tecnico.psia.g2.users;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(length = 500)
    private String bio;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_genres", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "genre", nullable = false, length = 32)
    private Set<Genre> favoriteGenres = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected User() {
    }

    // new user
    public User(String username, String passwordHash, String bio, Set<Genre> favoriteGenres) {
        // Postgres timestamptz keeps microseconds. Without truncating, the 201 body (nanoseconds)
        // and a later GET (rounded to micros) would show different createdAt values.
        this(UUID.randomUUID(), username, passwordHash, bio, favoriteGenres,
                Instant.now().truncatedTo(ChronoUnit.MICROS));
    }

    public User(UUID id, String username, String passwordHash, String bio, Set<Genre> favoriteGenres,
            Instant createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.bio = bio;
        this.favoriteGenres = new HashSet<>(favoriteGenres);
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

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Set<Genre> getFavoriteGenres() {
        return Set.copyOf(favoriteGenres);
    }

    public void setFavoriteGenres(Set<Genre> favoriteGenres) {
        this.favoriteGenres.clear();
        this.favoriteGenres.addAll(favoriteGenres);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
