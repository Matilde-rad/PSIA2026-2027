package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TasteProfile(String userId, String bio, List<String> favoriteGenres, List<String> favoriteDirectors,
        List<String> favoriteMovies, List<String> liked, List<String> disliked) {

    public TasteProfile {
        favoriteGenres = clean(favoriteGenres);
        favoriteDirectors = clean(favoriteDirectors);
        favoriteMovies = clean(favoriteMovies);
        liked = clean(liked);
        disliked = clean(disliked);
    }

    public TasteProfile(String userId, String bio, List<String> favoriteGenres, List<String> favoriteDirectors,
            List<String> favoriteMovies) {
        this(userId, bio, favoriteGenres, favoriteDirectors, favoriteMovies, List.of(), List.of());
    }

    public TasteProfile withWatched(List<String> liked, List<String> disliked) {
        return new TasteProfile(userId, bio, favoriteGenres, favoriteDirectors, favoriteMovies, liked, disliked);
    }

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

    public SemanticSearch.Query query(Map<String, String> titles) {
        Set<String> seen = new LinkedHashSet<>(favoriteMovies);
        seen.addAll(liked);
        seen.addAll(disliked);
        return new SemanticSearch.Query(text(titles), Set.copyOf(liked), Set.copyOf(disliked), Set.copyOf(seen));
    }

    private static List<String> clean(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(v -> v != null && !v.isBlank()).map(String::strip).toList();
    }
}
