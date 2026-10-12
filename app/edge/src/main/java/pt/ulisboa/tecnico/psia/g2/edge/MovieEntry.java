package pt.ulisboa.tecnico.psia.g2.edge;

// A movie on the caller's list as users reports it. status is to_watch, liked or disliked.
public record MovieEntry(String movieId, String status, String updatedAt) {
}
