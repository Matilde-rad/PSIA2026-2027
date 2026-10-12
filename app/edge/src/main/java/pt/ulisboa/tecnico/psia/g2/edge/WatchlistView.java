package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

// The caller's movies in two lists, each newest first: still to watch, and watched with a verdict.
public record WatchlistView(List<Item> toWatch, List<Item> watched) {

    // status is to_watch in the first list, liked or disliked in the second
    public record Item(String movieId, String title, Integer year, String posterUrl, String status,
            String updatedAt) {

        static Item of(MovieEntry entry, Movie movie) {
            return new Item(movie.movieId(), movie.title(), movie.year(), movie.posterUrl(), entry.status(),
                    entry.updatedAt());
        }
    }
}
