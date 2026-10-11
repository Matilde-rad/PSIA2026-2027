package pt.ulisboa.tecnico.psia.g2.recommendations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.ProcessingException;
import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.psia.g2.recommendations.SearchResponse.AppliedFilters;

class RecommendationAgentTest {

    static class FakeUsers implements UsersClient {
        List<UserTaste> rows = List.of();
        RuntimeException failure;

        @Override
        public List<UserTaste> list() {
            if (failure != null) {
                throw failure;
            }
            return rows;
        }
    }

    static class FakePipeline extends SearchPipeline {
        final List<String> searched = new ArrayList<>();
        final Map<String, List<Recommendation>> results = new HashMap<>();
        final Map<String, RuntimeException> failures = new HashMap<>();

        @Override
        public SearchResponse search(String text) {
            searched.add(text);
            if (failures.containsKey(text)) {
                throw failures.get(text);
            }
            return new SearchResponse(results.get(text), new AppliedFilters(List.of(), null, null));
        }
    }

    static class FakeCache extends RecommendationCache {
        record Write(String userId, List<Recommendation> items) {
        }

        final List<Write> writes = new ArrayList<>();

        @Override
        public void replace(String userId, List<Recommendation> items) {
            writes.add(new Write(userId, items));
        }
    }

    private final FakeUsers users = new FakeUsers();
    private final FakePipeline pipeline = new FakePipeline();
    private final FakeCache cache = new FakeCache();
    private final RecommendationAgent agent = new RecommendationAgent();

    RecommendationAgentTest() {
        agent.users = users;
        agent.pipeline = pipeline;
        agent.cache = cache;
    }

    private static Recommendation r(int id) {
        return new Recommendation(String.valueOf(id), "title-" + id, null, null);
    }

    @Test
    void oneFailingUserKeepsItsCacheAndTheNextUsersAreStillCached() {
        users.rows = List.of(
                new UsersClient.UserTaste("1", "a"),
                new UsersClient.UserTaste("2", "b"),
                new UsersClient.UserTaste("3", "c"));
        pipeline.results.put("a", List.of(r(10)));
        pipeline.failures.put("b", new SearchPipeline.AiUnavailableException(null));
        pipeline.results.put("c", List.of(r(30), r(31)));

        assertDoesNotThrow(agent::refreshAll);

        assertEquals(List.of("a", "b", "c"), pipeline.searched);
        assertEquals(
                List.of(
                        new FakeCache.Write("1", List.of(r(10))),
                        new FakeCache.Write("3", List.of(r(30), r(31)))),
                cache.writes);
    }

    @Test
    void emptyResultsAreNeverWritten() {
        users.rows = List.of(new UsersClient.UserTaste("1", "a"));
        pipeline.results.put("a", List.of());

        assertDoesNotThrow(agent::refreshAll);

        assertEquals(List.of("a"), pipeline.searched);
        assertTrue(cache.writes.isEmpty());
    }

    @Test
    void usersUnavailableWritesNothingAndDoesNotThrow() {
        users.failure = new ProcessingException("users down");

        assertDoesNotThrow(agent::refreshAll);

        assertTrue(pipeline.searched.isEmpty());
        assertTrue(cache.writes.isEmpty());
    }
}
