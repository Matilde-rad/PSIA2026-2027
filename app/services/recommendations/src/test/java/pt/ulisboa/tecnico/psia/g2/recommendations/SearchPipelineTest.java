package pt.ulisboa.tecnico.psia.g2.recommendations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.ProcessingException;
import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.psia.g2.recommendations.SearchResponse.AppliedFilters;

class SearchPipelineTest {

    static class FakeAi implements AiClient {
        Filters filters = new Filters(List.of(), null, null);
        RankResponse rankResponse;
        RuntimeException extractFailure;
        int extractCalls;
        int rankCalls;
        ExtractRequest lastExtract;
        RankRequest lastRank;

        @Override
        public Filters extractFilters(ExtractRequest request) {
            lastExtract = request;
            extractCalls++;
            if (extractFailure != null) {
                throw extractFailure;
            }
            return filters;
        }

        @Override
        public RankResponse rank(RankRequest request) {
            lastRank = request;
            rankCalls++;
            return rankResponse;
        }
    }

    static class FakeMovies implements MoviesClient {
        List<Candidate> rows = List.of();
        final List<List<Object>> calls = new ArrayList<>();

        @Override
        public List<String> genres() {
            return List.of("Action", "Comedy", "Horror");
        }

        @Override
        public List<Candidate> candidates(List<String> genres, Integer yearFrom, Integer yearTo, int limit) {
            calls.add(Arrays.asList(genres, yearFrom, yearTo));
            return rows;
        }
    }

    private final FakeAi ai = new FakeAi();
    private final FakeMovies movies = new FakeMovies();

    private SearchPipeline pipeline() {
        SearchPipeline pipeline = new SearchPipeline();
        pipeline.ai = ai;
        pipeline.movies = movies;
        pipeline.candidateCount = 15;
        pipeline.pickCount = 3;
        return pipeline;
    }

    private static MoviesClient.Candidate row(int id) {
        return new MoviesClient.Candidate(String.valueOf(id), "Title " + id + " (2000)", 2000, List.of("Drama"), 10);
    }

    private static AiClient.RankResponse picks(int... ids) {
        return new AiClient.RankResponse(Arrays.stream(ids)
                .mapToObj(id -> new AiClient.Pick(String.valueOf(id), "Title " + id + " (2000)"))
                .toList());
    }

    @Test
    void picksMapStraightToRecommendationsAfterExtract() {
        movies.rows = List.of(row(10), row(20), row(30));
        ai.rankResponse = picks(30, 10);

        SearchResponse response = pipeline().search(null);

        assertEquals(1, ai.extractCalls);
        assertEquals(new AiClient.ExtractRequest("", List.of("Action", "Comedy", "Horror")), ai.lastExtract);
        assertEquals(List.of(Arrays.asList(List.of(), null, null)), movies.calls);
        assertEquals("", ai.lastRank.request());
        assertEquals(3, ai.lastRank.limit());
        assertEquals(List.of("10", "20", "30"),
                ai.lastRank.candidates().stream().map(AiClient.RankCandidate::id).toList());
        assertEquals(List.of(
                new Recommendation("30", "Title 30 (2000)"),
                new Recommendation("10", "Title 10 (2000)")), response.results());
        assertEquals(new AppliedFilters(List.of(), null, null), response.appliedFilters());
    }

    @Test
    void extractFailureThrowsAiUnavailableAndNeverRanks() {
        ai.extractFailure = new ProcessingException("boom");

        assertThrows(SearchPipeline.AiUnavailableException.class, () -> pipeline().search("a horror movie"));
        assertEquals(0, ai.rankCalls);
        assertEquals(List.of(), movies.calls);
    }

    @Test
    void zeroPicksThrowsAiUnavailable() {
        movies.rows = List.of(row(10), row(20), row(30));
        ai.rankResponse = picks();

        assertThrows(SearchPipeline.AiUnavailableException.class, () -> pipeline().search(""));
        assertEquals(1, ai.rankCalls);
    }
}
