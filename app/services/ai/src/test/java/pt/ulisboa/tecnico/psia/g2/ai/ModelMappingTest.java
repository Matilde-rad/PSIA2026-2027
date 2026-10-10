package pt.ulisboa.tecnico.psia.g2.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.ConnectException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ProcessingException;
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
        assertEquals(List.of(
                new RankCandidates.Pick("20", "Same Title (1999)"),
                new RankCandidates.Pick("10", "Same Title (1999)"),
                new RankCandidates.Pick("30", "Third Title (2001)")), picks);
    }

    @Test
    void toFiltersKeepsOnlyExactAllowedGenresOnceInAllowedOrder() {
        ExtractFilters.Filters filters = ExtractFilters.toFilters(
                new ExtractFilters.ModelFilters(
                        Arrays.asList("Sci-Fi", "Horror", "Horror", "sci-fi", "Western2"), 0, 0, null),
                GENRES);
        assertEquals(List.of("Horror", "Sci-Fi"), filters.genres());
        assertNull(filters.yearFrom());
        assertNull(filters.yearTo());
        assertEquals(List.of(), filters.suggestions());
    }

    @Test
    void toFiltersTrimsDropsBlankDedupesAndCapsTitlesAtTen() {
        List<String> titles = new java.util.ArrayList<>(Arrays.asList(" Alien ", "", null, "Alien", "   "));
        for (int i = 1; i <= 11; i++) {
            titles.add("Film " + i);
        }
        ExtractFilters.Filters filters = ExtractFilters.toFilters(
                new ExtractFilters.ModelFilters(List.of(), 0, 0, titles), GENRES);
        assertEquals(List.of("Alien", "Film 1", "Film 2", "Film 3", "Film 4", "Film 5", "Film 6", "Film 7",
                "Film 8", "Film 9"), filters.suggestions());
    }

    @Test
    void parseAndToFiltersCarryTitles() {
        OllamaClient.ChatResponse response = reply("stop",
                "{\"genres\":[],\"titles\":[\"Alien\",\"Thelma & Louise\"],\"yearFrom\":0,\"yearTo\":0}");
        ExtractFilters.ModelFilters model =
                OllamaWrapper.parse(new ObjectMapper(), response, ExtractFilters.ModelFilters.class);
        ExtractFilters.Filters filters = ExtractFilters.toFilters(model, GENRES);
        assertEquals(List.of("Alien", "Thelma & Louise"), filters.suggestions());
        assertEquals(List.of(), filters.genres());
    }

    @Test
    void parseRejectsTruncatedReply() {
        OllamaClient.ChatResponse response = reply("length", "{\"genres\":[\"Horror\"");
        assertThrows(OllamaWrapper.ModelUnavailableException.class,
                () -> OllamaWrapper.parse(new ObjectMapper(), response, ExtractFilters.ModelFilters.class));
    }

    @Test
    void chatTurnsAClientFailureIntoModelUnavailableException() {
        OllamaWrapper wrapper = wrapper(request -> {
            throw new ProcessingException(new ConnectException("refused"));
        });
        assertThrows(OllamaWrapper.ModelUnavailableException.class,
                () -> wrapper.chat("extract", "s", "u", Map.of(), 10, ExtractFilters.ModelFilters.class));
    }

    private static OllamaWrapper wrapper(OllamaClient client) {
        OllamaWrapper wrapper = new OllamaWrapper();
        wrapper.client = client;
        wrapper.objectMapper = new ObjectMapper();
        wrapper.model = "m";
        wrapper.numCtx = 8192;
        wrapper.keepAlive = "30m";
        wrapper.numThread = Optional.empty();
        return wrapper;
    }

    private static OllamaClient.ChatResponse reply(String doneReason, String content) {
        return new OllamaClient.ChatResponse(
                new OllamaClient.Message("assistant", content), true, doneReason, null, null, null, null, null, null);
    }
}
