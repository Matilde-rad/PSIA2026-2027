package pt.ulisboa.tecnico.psia.g2.movies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MovieLensCsvTest {

    @Test
    void quotedTitleKeepsCommaAndSplitsGenresInFileOrder() {
        var row = MovieLensCsv.parseMovieLine("101,\"Example Film, The (1995)\",Comedy|Drama|Romance");
        assertEquals(101, row.id());
        assertEquals("Example Film, The (1995)", row.title());
        assertEquals(1995, row.year());
        assertEquals(List.of("Comedy", "Drama", "Romance"), row.genres());
    }

    @Test
    void doubledQuotesBecomeOneLiteralQuote() {
        var row = MovieLensCsv.parseMovieLine("102,\"10'05\"\"02 - A Title (2002)\",Drama");
        assertEquals("10'05\"02 - A Title (2002)", row.title());
        assertEquals(2002, row.year());
        assertEquals(List.of("Drama"), row.genres());
    }

    @Test
    void titleWithoutYearHasNullYear() {
        var row = MovieLensCsv.parseMovieLine("103,Untitled Series,Sci-Fi");
        assertEquals("Untitled Series", row.title());
        assertNull(row.year());
        assertEquals(List.of("Sci-Fi"), row.genres());
    }

    @Test
    void trailingSpaceIsStrippedAndYearComesFromLastGroupOnly() {
        var row = MovieLensCsv.parseMovieLine("104,Example (Alt Title) (2007) ,Action|Drama|War");
        assertEquals("Example (Alt Title) (2007)", row.title());
        assertEquals(2007, row.year());
        assertEquals(List.of("Action", "Drama", "War"), row.genres());
    }

    @Test
    void noGenresListedGivesEmptyList() {
        var row = MovieLensCsv.parseMovieLine("105,Some Road,(no genres listed)");
        assertNull(row.year());
        assertTrue(row.genres().isEmpty());
    }

    @Test
    void yearRangeIsNotAYearAndUtf8IsKept() {
        var row = MovieLensCsv.parseMovieLine("106,Série: Nôto (2006–2007),(no genres listed)");
        assertEquals("Série: Nôto (2006–2007)", row.title());
        assertNull(row.year());
        assertTrue(row.genres().isEmpty());
    }

    @Test
    void malformedLinesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> MovieLensCsv.parseMovieLine("107,Only Two Fields"));
        assertThrows(IllegalArgumentException.class, () -> MovieLensCsv.parseMovieLine("abc,Some Title (2001),Drama"));
    }

    @Test
    void titleKeyStripsYearMovesTrailingArticleAndLowercases() {
        assertEquals("the shawshank redemption", MovieLensCsv.titleKey("Shawshank Redemption, The (1994)"));
        assertEquals("the shawshank redemption", MovieLensCsv.titleKey("The Shawshank Redemption"));
        assertEquals("a star is born", MovieLensCsv.titleKey("Star Is Born, A (1954)"));
        assertEquals("la dolce vita", MovieLensCsv.titleKey("Dolce Vita, La (1960)"));
        assertEquals("the matrix", MovieLensCsv.titleKey("  The   MATRIX  "));
    }

    @Test
    void titleKeyDropsTrailingAlternateTitleButNeverEmptiesTheKey() {
        assertEquals("seven", MovieLensCsv.titleKey("Seven (a.k.a. Se7en) (1995)"));
        assertEquals("the city of lost children",
                MovieLensCsv.titleKey("City of Lost Children, The (Cité des enfants perdus, La) (1995)"));
        assertEquals("assembly", MovieLensCsv.titleKey("Assembly (Ji jie hao) (2007) "));
        assertEquals("(500) days of summer", MovieLensCsv.titleKey("(500) Days of Summer (2009)"));
        assertEquals("(untitled)", MovieLensCsv.titleKey("(Untitled) (2009)"));
    }

    @Test
    void titleKeyOfNullOrBlankIsEmptyAndTitlesWithoutYearAreKept() {
        assertEquals("", MovieLensCsv.titleKey(null));
        assertEquals("", MovieLensCsv.titleKey("   "));
        assertEquals("babylon 5", MovieLensCsv.titleKey("Babylon 5"));
    }
}
