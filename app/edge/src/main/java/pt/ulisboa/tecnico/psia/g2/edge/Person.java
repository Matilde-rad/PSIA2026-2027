package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

// Another user as users reports them to the caller. sharedMovies holds movie ids.
public record Person(String id, String username, String displayName, boolean followedByMe,
        List<String> sharedGenres, List<String> sharedDirectors, List<String> sharedMovies) {
}
