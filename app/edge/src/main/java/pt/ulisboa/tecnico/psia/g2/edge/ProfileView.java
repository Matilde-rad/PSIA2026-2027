package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

// profile is null until the user sets one up. favoriteMovies are the movies behind the ids in
// profile.favoriteMovies, with titles and posters. recommendations are only on your own screen.
public record ProfileView(String id, String username, String createdAt, Profile profile,
        List<Movie> favoriteMovies, long followers, long following, boolean followedByMe,
        List<Recommendation> recommendations) {
}
