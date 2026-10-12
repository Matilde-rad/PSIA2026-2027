package pt.ulisboa.tecnico.psia.g2.users;

import java.util.List;

// Another user as the caller sees them: who they are, whether the caller follows them, and what
// the two profiles have in common. sharedMovies holds movie ids, like Profile.favoriteMovies.
public record Person(String id, String username, String displayName, boolean followedByMe,
        List<String> sharedGenres, List<String> sharedDirectors, List<String> sharedMovies) {
}
