package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

public record WatchlistView(List<Item> toWatch, List<Item> watched) {

    public record Item(String movieId, String title, Integer year, String posterUrl, String status,
            String updatedAt) {

        static Item of(MovieEntry entry, Movie movie) {
            return new Item(movie.movieId(), movie.title(), movie.year(), movie.posterUrl(), entry.status(),
                    entry.updatedAt());
        }
    }
}
