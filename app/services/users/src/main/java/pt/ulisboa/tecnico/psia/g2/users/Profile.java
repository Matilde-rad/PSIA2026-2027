package pt.ulisboa.tecnico.psia.g2.users;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "profiles")
public class Profile extends PanacheEntityBase {

    @Id
    public UUID userId;

    public String displayName;

    public String bio;

    @JdbcTypeCode(SqlTypes.JSON)
    public List<String> favoriteGenres = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    public List<String> favoriteDirectors = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    public List<String> favoriteMovies = new ArrayList<>();
}
