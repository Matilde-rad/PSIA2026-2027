package pt.ulisboa.tecnico.psia.g2.users;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "profiles")
public class Profile extends PanacheEntityBase {

    static final int MAX_GENRES = 30;
    static final int MAX_FAVORITES = 10;
    static final int MAX_ENTRY_LENGTH = 80;

    @Id
    @Column(name = "user_id")
    public UUID userId;

    @Size(max = 64)
    @Column(name = "display_name", length = 64)
    public String displayName;

    @Size(max = 500)
    @Column(length = 500)
    public String bio;

    @Size(max = MAX_GENRES)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "favorite_genres", nullable = false)
    public List<@NotBlank @Size(max = MAX_ENTRY_LENGTH) String> favoriteGenres = new ArrayList<>();

    @Size(max = MAX_FAVORITES)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "favorite_directors", nullable = false)
    public List<@NotBlank @Size(max = MAX_ENTRY_LENGTH) String> favoriteDirectors = new ArrayList<>();

    @Size(max = MAX_FAVORITES)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "favorite_movies", nullable = false)
    public List<@NotNull @Pattern(regexp = "[0-9]{1,9}", message = "must be a movie id") String> favoriteMovies =
            new ArrayList<>();
}
