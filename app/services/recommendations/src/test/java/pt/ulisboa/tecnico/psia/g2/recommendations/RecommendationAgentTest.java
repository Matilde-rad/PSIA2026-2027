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

    static class FakeSearch extends SemanticSearch {
        final List<String> searched = new ArrayList<>();
        final Map<String, List<Recommendation>> results = new HashMap<>();
        final Map<String, RuntimeException> failures = new HashMap<>();

        @Override
        public SearchResponse search(String text) {
            searched.add(text);
            if (failures.containsKey(text)) {
                throw failures.get(text);
            }
            return new SearchResponse(results.get(text));
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
    private final FakeSearch search = new FakeSearch();
    private final FakeCache cache = new FakeCache();
    private final RecommendationAgent agent = new RecommendationAgent();

    RecommendationAgentTest() {
        agent.users = users;
        agent.semantic = search;
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
        search.results.put("a", List.of(r(10)));
        search.failures.put("b", new SemanticSearch.AiUnavailableException(null));
        search.results.put("c", List.of(r(30), r(31)));

        assertDoesNotThrow(agent::refreshAll);

        assertEquals(List.of("a", "b", "c"), search.searched);
        assertEquals(
                List.of(
                        new FakeCache.Write("1", List.of(r(10))),
                        new FakeCache.Write("3", List.of(r(30), r(31)))),
                cache.writes);
    }

    @Test
    void usersWithTheSameTasteShareOneSearch() {
        users.rows = List.of(
                new UsersClient.UserTaste("1", "a"),
                new UsersClient.UserTaste("2", "a"),
                new UsersClient.UserTaste("3", "b"));
        search.results.put("a", List.of(r(10)));
        search.results.put("b", List.of(r(20)));

        assertDoesNotThrow(agent::refreshAll);

        assertEquals(List.of("a", "b"), search.searched);
        assertEquals(
                List.of(
                        new FakeCache.Write("1", List.of(r(10))),
                        new FakeCache.Write("2", List.of(r(10))),
                        new FakeCache.Write("3", List.of(r(20)))),
                cache.writes);
    }

    @Test
    void emptyResultsAreNeverWritten() {
        users.rows = List.of(new UsersClient.UserTaste("1", "a"));
        search.results.put("a", List.of());

        assertDoesNotThrow(agent::refreshAll);

        assertEquals(List.of("a"), search.searched);
        assertTrue(cache.writes.isEmpty());
    }

    @Test
    void usersUnavailableWritesNothingAndDoesNotThrow() {
        users.failure = new ProcessingException("users down");

        assertDoesNotThrow(agent::refreshAll);

        assertTrue(search.searched.isEmpty());
        assertTrue(cache.writes.isEmpty());
    }
}
