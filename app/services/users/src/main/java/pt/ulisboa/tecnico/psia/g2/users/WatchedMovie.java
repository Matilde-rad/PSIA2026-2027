package pt.ulisboa.tecnico.psia.g2.users;

import java.time.Instant;

public record WatchedMovie(String userId, String movieId, boolean liked, Instant watchedAt) {
}
