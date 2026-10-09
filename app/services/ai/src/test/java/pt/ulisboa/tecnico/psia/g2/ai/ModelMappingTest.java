package pt.ulisboa.tecnico.psia.g2.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ModelMappingTest {

    private static final List<String> GENRES = List.of("Action", "Adventure", "Animation", "Children", "Comedy",
            "Crime", "Documentary", "Drama", "Fantasy", "Film-Noir", "Horror", "IMAX", "Musical", "Mystery",
            "Romance", "Sci-Fi", "Thriller", "War", "Western");

    private static final List<RankCandidates.Candidate> CANDIDATES = List.of(
            new RankCandidates.Candidate("10", "Same Title (1999)", List.of("Drama")),
            new RankCandidates.Candidate("20", "Same Title (1999)", List.of("Drama")),
            new RankCandidates.Candidate("30", "Third Title (2001)", List.of("Comedy")),
            new RankCandidates.Candidate("40", "Fourth Title (2002)", List.of("War")));

    private static final List<RankCandidates.ModelPick> PICKS = List.of(
            new RankCandidates.ModelPick(2),
            new RankCandidates.ModelPick(2),
            new RankCandidates.ModelPick(9),
            new RankCandidates.ModelPick(0),
            new RankCandidates.ModelPick(1),
            new RankCandidates.ModelPick(3));

    @Test
    void toPicksSkipsOutOfRangeAndRepeatedIndicesAndCapsAtLimit() {
        List<RankCandidates.Pick> picks = RankCandidates.toPicks(PICKS, CANDIDATES, 3);
        assertEquals(List.of("20", "10", "30"), picks.stream().map(RankCandidates.Pick::movieId).toList());
    }

    @Test
    void toPicksRespectsSmallerLimit() {
        List<RankCandidates.Pick> picks = RankCandidates.toPicks(PICKS, CANDIDATES, 2);
        assertEquals(List.of("20", "10"), picks.stream().map(RankCandidates.Pick::movieId).toList());
    }

    @Test
    void toPicksHandlesEmptyAndNullPickList() {
        assertEquals(List.of(), RankCandidates.toPicks(List.of(), CANDIDATES, 3));
        assertEquals(List.of(), RankCandidates.toPicks(null, CANDIDATES, 3));
    }

    @Test
    void modelPicksReadTheNumberFromTheFilmLabel() throws Exception {
        String json = "{\"picks\":[{\"film\":\"12. Seven (a.k.a. Se7en) (1995)\"},"
                + "{\"film\":\"no number\"},{\"film\":null}]}";
        RankCandidates.ModelPicks picks = new ObjectMapper().readValue(json, RankCandidates.ModelPicks.class);
        assertEquals(List.of(12, 0, 0), picks.picks().stream().map(RankCandidates.ModelPick::index).toList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void schemaAsksOnlyForAListedFilmLabel() {
        Map<String, Object> schema = RankCandidates.schema(CANDIDATES);
        Map<String, Object> picks = (Map<String, Object>) ((Map<String, Object>) schema.get("properties")).get("picks");
        Map<String, Object> items = (Map<String, Object>) picks.get("items");
        Map<String, Object> properties = (Map<String, Object>) items.get("properties");
        assertEquals(Set.of("film"), properties.keySet());
        assertEquals(List.of("film"), items.get("required"));
        Map<String, Object> film = (Map<String, Object>) properties.get("film");
        assertEquals(List.of("1. Same Title (1999)", "2. Same Title (1999)", "3. Third Title (2001)",
                "4. Fourth Title (2002)"), film.get("enum"));
    }

    @Test
    void responseRoundTripsPicksAsMovieIdOnly() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        RankCandidates.Response response = new RankCandidates.Response(List.of(new RankCandidates.Pick("20")));
        String json = mapper.writeValueAsString(response);
        assertEquals("{\"picks\":[{\"movieId\":\"20\"}]}", json);
        assertEquals(response, mapper.readValue(json, RankCandidates.Response.class));
    }

    @Test
    void toFiltersKeepsOnlyExactAllowedGenresOnceInAllowedOrder() {
        ExtractFilters.Filters filters = ExtractFilters.toFilters(
                new ExtractFilters.ModelFilters(
                        Arrays.asList("Sci-Fi", "Horror", "Horror", "sci-fi", "Western2"), 0, 0),
                GENRES);
        assertEquals(List.of("Horror", "Sci-Fi"), filters.genres());
        assertNull(filters.yearFrom());
        assertNull(filters.yearTo());
    }

    @Test
    void toFiltersKeepsSingleYearRangeAndTreatsNullGenresAsEmpty() {
        ExtractFilters.Filters filters = ExtractFilters.toFilters(
                new ExtractFilters.ModelFilters(null, 1995, 1995), GENRES);
        assertEquals(List.of(), filters.genres());
        assertEquals(1995, filters.yearFrom());
        assertEquals(1995, filters.yearTo());
    }

    @Test
    void parseRejectsTruncatedReply() {
        OllamaClient.ChatResponse response = reply("length", "{\"genres\":[\"Horror\"");
        assertThrows(OllamaWrapper.ModelOutputException.class,
                () -> OllamaWrapper.parse(new ObjectMapper(), response, ExtractFilters.ModelFilters.class));
    }

    @Test
    void parseRejectsInvalidJson() {
        OllamaClient.ChatResponse response = reply("stop", "{not json");
        assertThrows(OllamaWrapper.ModelOutputException.class,
                () -> OllamaWrapper.parse(new ObjectMapper(), response, ExtractFilters.ModelFilters.class));
    }

    @Test
    void parseRejectsMissingResponseOrMessage() {
        assertThrows(OllamaWrapper.ModelOutputException.class,
                () -> OllamaWrapper.parse(new ObjectMapper(), null, ExtractFilters.ModelFilters.class));
        OllamaClient.ChatResponse noMessage = new OllamaClient.ChatResponse(
                null, true, "stop", null, null, null, null, null, null);
        assertThrows(OllamaWrapper.ModelOutputException.class,
                () -> OllamaWrapper.parse(new ObjectMapper(), noMessage, ExtractFilters.ModelFilters.class));
    }

    @Test
    void parseReturnsTypedFiltersOnStop() {
        OllamaClient.ChatResponse response = reply("stop",
                "{\"genres\":[\"Horror\"],\"yearFrom\":1980,\"yearTo\":1989}");
        ExtractFilters.ModelFilters filters =
                OllamaWrapper.parse(new ObjectMapper(), response, ExtractFilters.ModelFilters.class);
        assertEquals(new ExtractFilters.ModelFilters(List.of("Horror"), 1980, 1989), filters);
    }

    private static OllamaClient.ChatResponse reply(String doneReason, String content) {
        return new OllamaClient.ChatResponse(
                new OllamaClient.Message("assistant", content), true, doneReason, null, null, null, null, null, null);
    }
}
