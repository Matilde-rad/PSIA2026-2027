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
}
