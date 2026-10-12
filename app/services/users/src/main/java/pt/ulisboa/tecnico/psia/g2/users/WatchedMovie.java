package pt.ulisboa.tecnico.psia.g2.users;

import java.time.Instant;

// A movie a user watched and what they thought of it, for other services.
public record WatchedMovie(String userId, String movieId, boolean liked, Instant watchedAt) {
}
