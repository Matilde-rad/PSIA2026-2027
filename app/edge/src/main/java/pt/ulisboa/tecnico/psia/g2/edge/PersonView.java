package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;
import java.util.Map;

public record PersonView(String id, String username, String displayName, boolean followedByMe,
        List<String> sharedGenres, List<String> sharedDirectors, List<String> sharedMovies) {

    static PersonView of(Person p, Map<String, Movie> movies) {
        return new PersonView(p.id(), p.username(), p.displayName(), p.followedByMe(), p.sharedGenres(),
                p.sharedDirectors(), MovieLookup.inOrder(p.sharedMovies(), movies).stream().map(Movie::title).toList());
    }
}
