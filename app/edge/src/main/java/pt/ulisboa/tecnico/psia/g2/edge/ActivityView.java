package pt.ulisboa.tecnico.psia.g2.edge;

// An Activity for the UI: the movie comes with its title and poster.
public record ActivityView(String userId, String username, String displayName, String status, String at,
        Movie movie) {
}
