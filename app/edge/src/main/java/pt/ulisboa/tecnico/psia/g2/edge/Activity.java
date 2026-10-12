package pt.ulisboa.tecnico.psia.g2.edge;

// Something a user did with a movie, as users reports it. status is to_watch, liked or disliked.
public record Activity(String userId, String username, String displayName, String movieId, String status,
        String at) {
}
