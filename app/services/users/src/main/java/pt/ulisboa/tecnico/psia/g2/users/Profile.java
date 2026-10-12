package pt.ulisboa.tecnico.psia.g2.users;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "profiles")
public class Profile extends PanacheEntityBase {

    // same id as the account in users
    @Id
    @Column(name = "user_id")
    public UUID userId;

    @Size(max = 64)
    @Column(name = "display_name", length = 64)
    public String displayName;

    @Size(max = 500)
    @Column(length = 500)
    public String bio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "favorite_genres", nullable = false)
    public List<String> favoriteGenres = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "favorite_directors", nullable = false)
    public List<String> favoriteDirectors = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "favorite_movies", nullable = false)
    public List<String> favoriteMovies = new ArrayList<>();
}
