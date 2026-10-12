package pt.ulisboa.tecnico.psia.g2.users;

import java.time.Instant;

// The latest thing a user did with a movie: put it on their watchlist, liked it or disliked it.
public record Activity(String userId, String username, String displayName, String movieId, String status,
        Instant at) {
}
