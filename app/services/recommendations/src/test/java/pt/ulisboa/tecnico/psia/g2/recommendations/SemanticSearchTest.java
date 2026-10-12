package pt.ulisboa.tecnico.psia.g2.recommendations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class SemanticSearchTest {

    private static MoviesClient.Similar movie(String id, int ratingCount, double similarity) {
        return new MoviesClient.Similar(id, "Title " + id, 2000, List.of("Drama"), ratingCount, null, null, similarity);
    }

    @Test
    void popularityTipsANearTie() {
        List<MoviesClient.Similar> ranked = SemanticSearch.rank(
                List.of(movie("obscure", 5, 0.61), movie("famous", 50000, 0.60)), 0.05);

        assertEquals(List.of("famous", "obscure"), ranked.stream().map(MoviesClient.Similar::id).toList());
    }

    @Test
    void popularityDoesNotOverrideAClearGap() {
        List<MoviesClient.Similar> ranked = SemanticSearch.rank(
                List.of(movie("famous", 50000, 0.40), movie("fits", 5, 0.60)), 0.05);

        assertEquals(List.of("fits", "famous"), ranked.stream().map(MoviesClient.Similar::id).toList());
    }

    @Test
    void favoritesAreDroppedById() {
        List<MoviesClient.Similar> kept = SemanticSearch.without(
                List.of(movie("1214", 100, 0.9), movie("1200", 100, 0.8), movie("2571", 100, 0.7)),
                List.of("1214", "2571"));

        assertEquals(List.of("1200"), kept.stream().map(MoviesClient.Similar::id).toList());
    }
}
