package pt.ulisboa.tecnico.psia.g2.recommendations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.ProcessingException;
import org.junit.jupiter.api.Test;

class RecommendationAgentTest {

    static class FakeUsers implements UsersClient {
        List<TasteProfile> rows = List.of();
        RuntimeException failure;
        String keySent;

        @Override
        public List<TasteProfile> profiles(String internalKey) {
            keySent = internalKey;
            if (failure != null) {
                throw failure;
            }
            return rows;
        }
    }

    static class FakeSearch extends SemanticSearch {
        final List<String> searched = new ArrayList<>();
        final List<Collection<String>> excluded = new ArrayList<>();
        final Map<String, List<Recommendation>> results = new HashMap<>();
        final Map<String, RuntimeException> failures = new HashMap<>();

        @Override
        public SearchResponse search(String text, Collection<String> excludeTitles) {
            searched.add(text);
            excluded.add(excludeTitles);
            if (failures.containsKey(text)) {
                throw failures.get(text);
            }
            return new SearchResponse(results.get(text));
        }
    }

    static class FakeTitles extends MovieTitles {
        final Map<String, String> known = new HashMap<>();
        final List<Collection<String>> asked = new ArrayList<>();
        RuntimeException failure;

        @Override
        public Map<String, String> of(Collection<String> ids) {
            asked.add(ids);
            if (failure != null) {
                throw failure;
            }
            return known;
        }
    }

    static class FakeCache extends RecommendationCache {
        record Write(String userId, List<Recommendation> items) {
        }

        final List<Write> writes = new ArrayList<>();
        final Map<String, List<Recommendation>> stored = new HashMap<>();

        @Override
        public void replace(String userId, List<Recommendation> items) {
            writes.add(new Write(userId, items));
        }

        @Override
        public List<Recommendation> read(String userId) {
            return stored.getOrDefault(userId, List.of());
        }
    }

    private final FakeUsers users = new FakeUsers();
    private final FakeSearch search = new FakeSearch();
    private final FakeCache cache = new FakeCache();
    private final FakeTitles titles = new FakeTitles();
    private final RecommendationAgent agent = new RecommendationAgent();

    RecommendationAgentTest() {
        agent.users = users;
        agent.semantic = search;
        agent.cache = cache;
        agent.movieTitles = titles;
        agent.internalKey = "key";
    }

    private static Recommendation r(int id) {
        return new Recommendation(String.valueOf(id), "title-" + id, null, null);
    }

    // a profile whose taste text is exactly its bio
    private static TasteProfile taste(String userId, String bio) {
        return new TasteProfile(userId, bio, List.of(), List.of(), List.of());
    }

    @Test
    void oneFailingUserKeepsItsCacheAndTheNextUsersAreStillCached() {
        users.rows = List.of(taste("1", "a"), taste("2", "b"), taste("3", "c"));
        search.results.put("a", List.of(r(10)));
        search.failures.put("b", new SemanticSearch.AiUnavailableException(null));
        search.results.put("c", List.of(r(30), r(31)));

        assertDoesNotThrow(agent::refreshAll);

        assertEquals("key", users.keySent);
        assertEquals(List.of("a", "b", "c"), search.searched);
        assertEquals(
                List.of(
                        new FakeCache.Write("1", List.of(r(10))),
                        new FakeCache.Write("3", List.of(r(30), r(31)))),
                cache.writes);
    }

    @Test
    void usersWithTheSameTasteShareOneSearch() {
        users.rows = List.of(taste("1", "a"), taste("2", "a"), taste("3", "b"));
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
    void aProfileWithoutTasteIsNotSearched() {
        users.rows = List.of(taste("1", null), taste("2", "  "), taste("3", "a"));
        search.results.put("a", List.of(r(10)));

        assertDoesNotThrow(agent::refreshAll);

        assertEquals(List.of("a"), search.searched);
        assertEquals(List.of(new FakeCache.Write("3", List.of(r(10)))), cache.writes);
    }

    @Test
    void theProfileBecomesTheSearchTextAndItsFavoriteMoviesAreExcluded() {
        TasteProfile profile = new TasteProfile("1", "Late-night fan.", List.of("Horror", "Sci-Fi"),
                List.of("Ridley Scott"), List.of("1214", "404"));
        titles.known.put("1214", "Alien (1979)");   // 404 is not in the catalog
        String text = "Horror, Sci-Fi movies. Directed by Ridley Scott. Similar to Alien (1979). Late-night fan.";
        users.rows = List.of(profile, taste("2", "b"));
        search.results.put(text, List.of(r(10)));
        search.results.put("b", List.of(r(20)));

        assertDoesNotThrow(agent::refreshAll);

        // every profile's favorites are resolved in one lookup
        assertEquals(List.of(List.of("1214", "404")), titles.asked);
        assertEquals(List.of(text, "b"), search.searched);
        assertEquals(List.of(List.of("1214", "404"), List.of()), search.excluded);
    }

    @Test
    void moviesUnavailableWritesNothingAndDoesNotThrow() {
        users.rows = List.of(taste("1", "a"));
        titles.failure = new ProcessingException("movies down");

        assertDoesNotThrow(agent::refreshAll);

        assertTrue(search.searched.isEmpty());
        assertTrue(cache.writes.isEmpty());
    }

    @Test
    void emptyResultsAreNeverWritten() {
        users.rows = List.of(taste("1", "a"));
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

    @Test
    void refreshCachesOneUsersPicks() {
        search.results.put("a", List.of(r(10)));

        assertEquals(List.of(r(10)), agent.refresh("1", taste("ignored", "a")));

        assertEquals(List.of(new FakeCache.Write("1", List.of(r(10)))), cache.writes);
    }

    @Test
    void refreshWithoutTasteReturnsTheCacheUntouched() {
        cache.stored.put("1", List.of(r(7)));

        assertEquals(List.of(r(7)), agent.refresh("1", taste("1", "")));
        assertEquals(List.of(r(7)), agent.refresh("1", null));

        assertTrue(search.searched.isEmpty());
        assertTrue(cache.writes.isEmpty());
    }

    @Test
    void refreshKeepsTheCacheWhenMoviesIsDown() {
        titles.failure = new ProcessingException("movies down");

        assertThrows(SemanticSearch.AiUnavailableException.class, () -> agent.refresh("1", taste("1", "a")));

        assertTrue(search.searched.isEmpty());
        assertTrue(cache.writes.isEmpty());
    }

    @Test
    void refreshKeepsTheCacheWhenTheAiIsDown() {
        search.failures.put("a", new SemanticSearch.AiUnavailableException(null));

        assertThrows(SemanticSearch.AiUnavailableException.class, () -> agent.refresh("1", taste("1", "a")));

        assertTrue(cache.writes.isEmpty());
    }

    @Test
    void moreSearchesTheTasteWithFavoritesAndShownIdsExcludedAndCachesNothing() {
        TasteProfile profile = new TasteProfile("1", null, List.of("Horror"), List.of(), List.of("1214", "404"));
        titles.known.put("1214", "Alien (1979)");   // 404 is not in the catalog
        String text = "Horror movies. Similar to Alien (1979)";
        search.results.put(text, List.of(r(10)));

        assertEquals(List.of(r(10)), agent.more(profile, List.of("7", "1214")));

        assertEquals(List.of(text), search.searched);
        assertEquals(Set.of("1214", "404", "7"), Set.copyOf(search.excluded.get(0)));
        assertTrue(cache.writes.isEmpty());
    }

    @Test
    void moreWithoutTasteSearchesNothing() {
        assertTrue(agent.more(taste("1", ""), List.of()).isEmpty());
        assertTrue(agent.more(null, List.of()).isEmpty());

        assertTrue(search.searched.isEmpty());
        assertTrue(cache.writes.isEmpty());
    }
}
