package pt.ulisboa.tecnico.psia.g2.users;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record UserView(String id, String username, String bio, List<Genre> favoriteGenres, Instant createdAt) {

    public static UserView from(User user) {
        List<Genre> genres = user.getFavoriteGenres().stream()
                .sorted(Comparator.comparing(Genre::name))
                .toList();
        return new UserView(user.getId().toString(), user.getUsername(), user.getBio(), genres, user.getCreatedAt());
    }
}
