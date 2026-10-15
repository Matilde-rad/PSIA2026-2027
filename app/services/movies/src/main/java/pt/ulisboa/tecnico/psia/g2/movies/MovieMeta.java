package pt.ulisboa.tecnico.psia.g2.movies;

import java.util.List;

public record MovieMeta(String description, Integer runtimeMinutes, List<String> cast, List<String> directors,
        Double imdbRating) {
}
