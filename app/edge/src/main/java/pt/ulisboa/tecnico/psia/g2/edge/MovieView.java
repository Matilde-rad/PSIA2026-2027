package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

// The movie page. description, runtimeMinutes and imdbRating are null and the cast and directors
// empty when Cinemeta has nothing on the film. status is the caller's own: to_watch, liked,
// disliked, or null when the movie is not on their list.
public record MovieView(String movieId, String title, Integer year, List<String> genres, String posterUrl,
        String bannerUrl, String description, Integer runtimeMinutes, List<String> cast, List<String> directors,
        Double imdbRating, String status) {

    static MovieView of(MoviesClient.MovieDetails m, MovieEntry entry) {
        return new MovieView(m.id(), m.title(), m.year(), m.genres(), m.posterUrl(), m.bannerUrl(),
                m.description(), m.runtimeMinutes(), m.cast(), m.directors(), m.imdbRating(),
                entry == null ? null : entry.status());
    }
}
