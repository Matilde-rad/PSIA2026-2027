package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// The part of a users profile that says what someone likes to watch.
// favoriteMovies holds movie catalog ids, not titles.
@JsonIgnoreProperties(ignoreUnknown = true)
public record TasteProfile(String userId, String bio, List<String> favoriteGenres, List<String> favoriteDirectors,
        List<String> favoriteMovies) {

    public TasteProfile {
        favoriteGenres = clean(favoriteGenres);
        favoriteDirectors = clean(favoriteDirectors);
        favoriteMovies = clean(favoriteMovies);
    }

    // The profile as one search request, in the words a person would type into Search.
    // Empty when the profile says nothing about taste. titles maps movie id to title; a favorite
    // the catalog does not have is left out.
    public String text(Map<String, String> titles) {
        List<String> parts = new ArrayList<>();
        if (!favoriteGenres.isEmpty()) {
            parts.add(String.join(", ", favoriteGenres) + " movies");
        }
        if (!favoriteDirectors.isEmpty()) {
            parts.add("Directed by " + String.join(", ", favoriteDirectors));
        }
        List<String> movies = favoriteMovies.stream().map(titles::get).filter(Objects::nonNull).toList();
        if (!movies.isEmpty()) {
            parts.add("Similar to " + String.join(", ", movies));
        }
        if (bio != null && !bio.isBlank()) {
            parts.add(bio.strip());
        }
        return String.join(". ", parts);
    }

    private static List<String> clean(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(v -> v != null && !v.isBlank()).map(String::strip).toList();
    }
}
