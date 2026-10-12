package pt.ulisboa.tecnico.psia.g2.movies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class CinemetaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static MovieMeta parse(String json) throws Exception {
        return Cinemeta.parse(MAPPER.readTree(json)).orElseThrow();
    }

    @Test
    void readsSynopsisRuntimeCastDirectorsAndRating() throws Exception {
        MovieMeta meta = parse("""
                {"meta": {"name": "Alien", "description": " A crew meets a deadly lifeform. ",
                  "runtime": "117 min", "cast": ["Sigourney Weaver", "Tom Skerritt"],
                  "director": ["Ridley Scott"], "imdbRating": "8.4", "writer": ["Dan O&apos;Bannon"]}}
                """);

        assertEquals("A crew meets a deadly lifeform.", meta.description());
        assertEquals(117, meta.runtimeMinutes());
        assertEquals(List.of("Sigourney Weaver", "Tom Skerritt"), meta.cast());
        assertEquals(List.of("Ridley Scott"), meta.directors());
        assertEquals(8.4, meta.imdbRating());
    }

    @Test
    void htmlEntitiesInNamesAreUnescaped() throws Exception {
        MovieMeta meta = parse("""
                {"meta": {"cast": ["Dan O&apos;Bannon", "Tom &amp; Jerry"], "director": null}}
                """);

        assertEquals(List.of("Dan O'Bannon", "Tom & Jerry"), meta.cast());
        assertTrue(meta.directors().isEmpty());
    }

    @Test
    void missingOrOddFieldsBecomeNullOrEmpty() throws Exception {
        MovieMeta meta = parse("""
                {"meta": {"name": "Obscure", "runtime": "N/A", "imdbRating": "", "cast": "nobody"}}
                """);

        assertNull(meta.description());
        assertNull(meta.runtimeMinutes());
        assertNull(meta.imdbRating());
        assertTrue(meta.cast().isEmpty());
    }

    @Test
    void anAnswerWithoutMetaIsEmpty() throws Exception {
        assertTrue(Cinemeta.parse(MAPPER.readTree("{}")).isEmpty());
        assertTrue(Cinemeta.parse(MAPPER.readTree("{\"meta\": {}}")).isEmpty());
        assertTrue(Cinemeta.parse(null).isEmpty());
    }

    @Test
    void runtimeIsReadInMinutes() {
        assertEquals(136, Cinemeta.minutes("136 min"));
        assertEquals(117, Cinemeta.minutes("1h 57min"));
        assertEquals(120, Cinemeta.minutes("2h"));
        assertNull(Cinemeta.minutes("0 min"));
        assertNull(Cinemeta.minutes(null));
    }
}
