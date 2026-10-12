package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

public record Profile(String userId, String displayName, String bio, List<String> favoriteGenres,
                      List<String> favoriteDirectors, List<String> favoriteMovies) {
}
