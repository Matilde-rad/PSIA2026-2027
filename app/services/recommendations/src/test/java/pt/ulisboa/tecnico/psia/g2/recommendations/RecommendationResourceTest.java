package pt.ulisboa.tecnico.psia.g2.recommendations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class RecommendationResourceTest {

    @Test
    void homeDrawsTwelveOfTheCachedPoolAtRandomOnEveryVisit() {
        List<Recommendation> pool = IntStream.range(0, 24)
                .mapToObj(i -> new Recommendation(String.valueOf(i), "title-" + i, null, null)).toList();
        RecommendationAgentTest.FakeCache cache = new RecommendationAgentTest.FakeCache();
        cache.stored.put("1", pool);
        SemanticSearch semantic = new SemanticSearch();
        semantic.pickCount = 12;
        semantic.poolSize = 24;
        RecommendationResource resource = new RecommendationResource();
        resource.cache = cache;
        resource.semantic = semantic;

        Set<List<Recommendation>> draws = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            List<Recommendation> draw = resource.forUser("1");
            assertEquals(12, draw.size());
            assertEquals(12, draw.stream().map(Recommendation::movieId).distinct().count());
            assertTrue(pool.containsAll(draw));
            draws.add(draw);
        }
        assertTrue(draws.size() > 1);
    }
}
