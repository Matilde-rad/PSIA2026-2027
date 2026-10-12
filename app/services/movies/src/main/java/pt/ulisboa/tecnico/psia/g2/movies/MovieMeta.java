package pt.ulisboa.tecnico.psia.g2.movies;

import java.util.List;

// What MovieLens does not have: synopsis, running time, cast, directors and the IMDb rating.
public record MovieMeta(String description, Integer runtimeMinutes, List<String> cast, List<String> directors,
        Double imdbRating) {
}
