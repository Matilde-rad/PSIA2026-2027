package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

public record MovieView(String movieId, String title, Integer year, List<String> genres, String posterUrl,
        String bannerUrl, String description, Integer runtimeMinutes, List<String> cast, List<String> directors,
        Double imdbRating, String status) {

    static MovieView of(MoviesClient.MovieDetails m, MovieEntry entry) {
        return new MovieView(m.id(), m.title(), m.year(), m.genres(), m.posterUrl(), m.bannerUrl(),
                m.description(), m.runtimeMinutes(), m.cast(), m.directors(), m.imdbRating(),
                entry == null ? null : entry.status());
    }
}
