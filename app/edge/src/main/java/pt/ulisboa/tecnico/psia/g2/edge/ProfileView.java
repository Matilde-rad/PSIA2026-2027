package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

public record ProfileView(String id, String username, String createdAt, Profile profile,
        List<Movie> favoriteMovies, long followers, long following, boolean followedByMe,
        List<Recommendation> recommendations) {
}
