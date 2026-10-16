package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class SemanticSearch {

    public static class AiUnavailableException extends RuntimeException {
        public AiUnavailableException(Throwable cause) {
            super(cause);
        }
    }

    public record Query(String text, Set<String> liked, Set<String> disliked, Set<String> exclude) {


        public boolean isEmpty() {
            return text.isEmpty() && liked.isEmpty();
        }

        public Query excluding(Collection<String> more) {
            Set<String> all = new HashSet<>(exclude);
            all.addAll(more);
            return new Query(text, liked, disliked, Set.copyOf(all));
        }

        public String fingerprint() {
            StringBuilder canonical = new StringBuilder();
            token(canonical, text);
            for (Set<String> ids : List.of(liked, disliked, exclude)) {
                token(canonical, String.valueOf(ids.size()));
                ids.stream().sorted().forEach(id -> token(canonical, id));
            }
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256")
                        .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
                return HexFormat.of().formatHex(digest);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }

        private static void token(StringBuilder out, String value) {
            out.append(value.length()).append(':').append(value);
        }
    }

    @Inject
    @RestClient
    AiClient ai;

    @Inject
    @RestClient
    MoviesClient movies;

    @ConfigProperty(name = "recs.semantic.candidates")
    int candidateCount;

    @ConfigProperty(name = "recs.search.picks")
    int pickCount;

    @ConfigProperty(name = "recs.semantic.popularity-weight")
    double popularityWeight;

    @ConfigProperty(name = "recs.semantic.min-ratings")
    int minRatings;

    @ConfigProperty(name = "recs.recommend.pool")
    int poolSize;

    @ConfigProperty(name = "recs.recommend.like-weight")
    double likeWeight;

    @ConfigProperty(name = "recs.recommend.dislike-weight")
    double dislikeWeight;

    final Random random = new Random();

    private static final int EMBEDDING_CACHE_SIZE = 1000;

    private final Map<String, float[]> embeddingCache = new ConcurrentHashMap<>();

    public SearchResponse search(String text) {
        return search(text, List.of());
    }

    // excludeIds: favorites, which would otherwise come back first.
    public SearchResponse search(String text, Collection<String> excludeIds) {
        try {
            return run(text == null ? "" : text, excludeIds);
        } catch (RuntimeException e) {
            throw new AiUnavailableException(e);
        }
    }

    private SearchResponse run(String text, Collection<String> excludeIds) {
        float[] query = embed(text);
        List<MoviesClient.Similar> near = movies.similar(new MoviesClient.SimilarRequest(query, candidateCount, minRatings));
        List<Recommendation> results = rank(without(near, excludeIds), popularityWeight).stream()
                .limit(pickCount)
                .map(s -> new Recommendation(s.id(), s.title(), s.posterUrl(), s.bannerUrl()))
                .toList();
        return new SearchResponse(results);
    }

    public List<Recommendation> pool(Query query) {
        if (query.isEmpty()) {
            return List.of();
        }
        try {
            return runPool(query);
        } catch (RuntimeException e) {
            throw new AiUnavailableException(e);
        }
    }

    public List<Recommendation> picks(List<Recommendation> pool) {
        return sample(pool, poolSize, pickCount, random);
    }

    public List<Recommendation> recommend(Query query) {
        return picks(pool(query));
    }

    private List<Recommendation> runPool(Query query) {
        float[] text = query.text().isEmpty() ? null : embed(query.text());
        Map<String, float[]> vectors = vectors(query);
        float[] vector = feedback(text, of(vectors, query.liked()), of(vectors, query.disliked()), likeWeight,
                dislikeWeight);
        if (vector == null) {
            return List.of();
        }
        List<MoviesClient.Similar> near = movies.similar(new MoviesClient.SimilarRequest(vector, candidateCount, minRatings));
        return rank(without(near, query.exclude()), popularityWeight).stream()
                .limit(poolSize)
                .map(s -> new Recommendation(s.id(), s.title(), s.posterUrl(), s.bannerUrl()))
                .toList();
    }

    private float[] embed(String text) {
        float[] vector = embeddingCache.get(text);
        if (vector == null) {
            vector = ai.embedQuery(new AiClient.EmbedQuery(text)).embedding();
            if (embeddingCache.size() >= EMBEDDING_CACHE_SIZE) {
                embeddingCache.clear();
            }
            embeddingCache.put(text, vector);
        }
        return vector;
    }

    private Map<String, float[]> vectors(Query query) {
        Set<String> ids = new HashSet<>(query.liked());
        ids.addAll(query.disliked());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return movies.vectors(new MoviesClient.LookupRequest(List.copyOf(ids))).stream()
                .collect(Collectors.toMap(MoviesClient.Vector::id, MoviesClient.Vector::embedding, (a, b) -> a));
    }

    private static List<float[]> of(Map<String, float[]> vectors, Set<String> ids) {
        return ids.stream().map(vectors::get).filter(v -> v != null).toList();
    }

    static float[] feedback(float[] text, List<float[]> liked, List<float[]> disliked, double likeWeight,
            double dislikeWeight) {
        float[] likedMean = mean(liked);
        if (text == null && likedMean == null) {
            return null;
        }
        float[] query = new float[text != null ? text.length : likedMean.length];
        add(query, text, 1);
        add(query, likedMean, likeWeight);
        add(query, mean(disliked), -dislikeWeight);
        return query;
    }

    private static float[] mean(List<float[]> vectors) {
        if (vectors.isEmpty()) {
            return null;
        }
        float[] sum = new float[vectors.get(0).length];
        vectors.forEach(v -> add(sum, v, 1));
        return sum;
    }

    private static void add(float[] target, float[] v, double weight) {
        if (v == null) {
            return;
        }
        double norm = 0;
        for (float x : v) {
            norm += x * x;
        }
        if (norm == 0) {
            return;
        }
        double scale = weight / Math.sqrt(norm);
        for (int i = 0; i < target.length; i++) {
            target[i] += (float) (v[i] * scale);
        }
    }

    static <T> List<T> sample(List<T> ranked, int pool, int count, Random random) {
        List<T> best = new ArrayList<>(ranked.subList(0, Math.min(pool, ranked.size())));
        Collections.shuffle(best, random);
        return List.copyOf(best.subList(0, Math.min(count, best.size())));
    }

    static List<MoviesClient.Similar> without(List<MoviesClient.Similar> near, Collection<String> excludeIds) {
        Set<String> excluded = Set.copyOf(excludeIds);
        return near.stream().filter(s -> !excluded.contains(s.id())).toList();
    }

    // Similarity decides; popularity only tips near-ties toward films people know.
    static List<MoviesClient.Similar> rank(List<MoviesClient.Similar> near, double popularityWeight) {
        double maxPopularity = near.stream().mapToDouble(s -> Math.log1p(s.ratingCount())).max().orElse(0);
        Comparator<MoviesClient.Similar> byScore = Comparator.comparingDouble(s -> s.similarity()
                + (maxPopularity == 0 ? 0 : popularityWeight * Math.log1p(s.ratingCount()) / maxPopularity));
        return near.stream().sorted(byScore.reversed()).toList();
    }
}
