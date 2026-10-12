package pt.ulisboa.tecnico.psia.g2.users;

import java.time.Instant;

// A movie on someone's list. status is to_watch, liked or disliked; the last two mean watched.
// movieId is an id of the movies service catalog, like Profile.favoriteMovies.
public record MovieEntry(String movieId, String status, Instant updatedAt) {
}
