package pt.ulisboa.tecnico.psia.g2.movies;

import java.util.List;

// A catalog movie plus its Cinemeta data. Without that data the description, runtime and rating
// are null and the lists empty.
public record MovieDetails(String id, String title, Integer year, List<String> genres, int ratingCount,
        String posterUrl, String bannerUrl, String description, Integer runtimeMinutes, List<String> cast,
        List<String> directors, Double imdbRating) {

    static MovieDetails of(MovieSummary m, MovieMeta meta) {
        return new MovieDetails(m.id(), m.title(), m.year(), m.genres(), m.ratingCount(), m.posterUrl(),
                m.bannerUrl(),
                meta == null ? null : meta.description(),
                meta == null ? null : meta.runtimeMinutes(),
                meta == null ? List.of() : meta.cast(),
                meta == null ? List.of() : meta.directors(),
                meta == null ? null : meta.imdbRating());
    }
}
