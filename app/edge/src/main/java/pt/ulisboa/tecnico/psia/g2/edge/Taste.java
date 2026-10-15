package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

// What recommendations searches from: the caller's profile and the movies they marked as watched.
public record Taste(String userId, String bio, List<String> favoriteGenres, List<String> favoriteDirectors,
        List<String> favoriteMovies, List<String> liked, List<String> disliked) {

    static Taste of(Profile profile, List<MovieEntry> watchlist) {
        return new Taste(profile.userId(), profile.bio(), profile.favoriteGenres(), profile.favoriteDirectors(),
                profile.favoriteMovies(), withStatus(watchlist, "liked"), withStatus(watchlist, "disliked"));
    }

    private static List<String> withStatus(List<MovieEntry> watchlist, String status) {
        return watchlist.stream().filter(e -> status.equals(e.status())).map(MovieEntry::movieId).toList();
    }
}
