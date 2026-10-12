package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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

    public SearchResponse search(String text) {
        return search(text, List.of());
    }

    // excludeIds: movies the user already named as favorites, which would otherwise come back first.
    public SearchResponse search(String text, Collection<String> excludeIds) {
        try {
            return run(text == null ? "" : text, excludeIds);
        } catch (RuntimeException e) {
            throw new AiUnavailableException(e);
        }
    }

    private SearchResponse run(String text, Collection<String> excludeIds) {
        float[] query = ai.embedQuery(new AiClient.EmbedQuery(text)).embedding();
        List<MoviesClient.Similar> near = movies.similar(new MoviesClient.SimilarRequest(query, candidateCount, minRatings));
        List<Recommendation> results = rank(without(near, excludeIds), popularityWeight).stream()
                .limit(pickCount)
                .map(s -> new Recommendation(s.id(), s.title(), s.posterUrl(), s.bannerUrl()))
                .toList();
        return new SearchResponse(results);
    }

    static List<MoviesClient.Similar> without(List<MoviesClient.Similar> near, Collection<String> excludeIds) {
        Set<String> excluded = Set.copyOf(excludeIds);
        return near.stream().filter(s -> !excluded.contains(s.id())).toList();
    }

    // Similarity decides; popularity (log of the rating count, scaled to the most-rated of these
    // movies) only tips near-ties toward films people know.
    static List<MoviesClient.Similar> rank(List<MoviesClient.Similar> near, double popularityWeight) {
        double maxPopularity = near.stream().mapToDouble(s -> Math.log1p(s.ratingCount())).max().orElse(0);
        Comparator<MoviesClient.Similar> byScore = Comparator.comparingDouble(s -> s.similarity()
                + (maxPopularity == 0 ? 0 : popularityWeight * Math.log1p(s.ratingCount()) / maxPopularity));
        return near.stream().sorted(byScore.reversed()).toList();
    }
}
