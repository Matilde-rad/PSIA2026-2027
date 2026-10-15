package pt.ulisboa.tecnico.psia.g2.movies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CatalogBrowseTest {

    @Inject
    CatalogRepository catalog;

    private static List<String> ids(List<MovieSummary> movies) {
        return movies.stream().map(MovieSummary::id).toList();
    }

    // "Return to the Matrix" has the most ratings but does not start with the text
    @Test
    void textMatchesTitleWordsAndTitleStartsComeFirst() {
        assertEquals(List.of("1", "2", "3"), ids(catalog.browse("matrix", List.of(), 0, 12)));
        assertEquals(List.of("1", "2", "3"), ids(catalog.search("matrix", 12)));
    }

    // 4 and 5 tie on ratings, so the id decides
    @Test
    void everyChosenGenreMustMatchMostRatedFirst() {
        assertEquals(List.of("3", "4", "5"), ids(catalog.browse(null, List.of("Comedy", "Sci-Fi"), 0, 12)));
        assertEquals(List.of("3"), ids(catalog.browse("matrix", List.of("Comedy"), 0, 12)));
    }

    @Test
    void pagesFollowEachOtherWithoutOverlap() {
        assertEquals(List.of("3", "1"), ids(catalog.browse(null, List.of("Sci-Fi"), 0, 2)));
        assertEquals(List.of("4", "5"), ids(catalog.browse(null, List.of("Sci-Fi"), 2, 2)));
        assertEquals(List.of("2"), ids(catalog.browse(null, List.of("Sci-Fi"), 4, 2)));
        assertEquals(List.of(), ids(catalog.browse(null, List.of("Sci-Fi"), 6, 2)));
    }

    @Test
    void unknownGenresAndEmptyQueriesMatchNothing() {
        assertEquals(List.of(), ids(catalog.browse(null, List.of("Nope"), 0, 12)));
        assertEquals(List.of(), ids(catalog.browse(null, List.of("Sci-Fi", "Nope"), 0, 12)));
        assertEquals(List.of(), ids(catalog.browse("  ", List.of(), 0, 12)));
    }
}
