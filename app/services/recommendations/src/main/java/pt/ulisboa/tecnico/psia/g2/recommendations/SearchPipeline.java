package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import pt.ulisboa.tecnico.psia.g2.recommendations.SearchResponse.AppliedFilters;

@ApplicationScoped
public class SearchPipeline {

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

    @ConfigProperty(name = "recs.search.candidates")
    int candidateCount;

    @ConfigProperty(name = "recs.search.picks")
    int pickCount;

    public SearchResponse search(String text) {
        try {
            return run(text == null ? "" : text);
        } catch (RuntimeException e) {
            throw new AiUnavailableException(e);
        }
    }

    private SearchResponse run(String text) {
        AiClient.Filters filters = ai.extractFilters(new AiClient.ExtractRequest(text, movies.genres()));
        AppliedFilters applied = new AppliedFilters(filters.genres(), filters.yearFrom(), filters.yearTo());

        List<MoviesClient.Candidate> found =
                movies.candidates(filters.genres(), filters.yearFrom(), filters.yearTo(), candidateCount);
        if (found.isEmpty()) {
            return new SearchResponse(List.of(), applied);
        }

        List<AiClient.RankCandidate> rankCandidates = found.stream()
                .map(c -> new AiClient.RankCandidate(c.id(), c.title(), c.genres()))
                .toList();
        AiClient.RankResponse picks = ai.rank(new AiClient.RankRequest(text, pickCount, rankCandidates));

        List<Recommendation> results =
                picks.picks().stream().map(p -> new Recommendation(p.movieId(), p.title())).toList();
        if (results.isEmpty()) {
            throw new AiUnavailableException(null);
        }
        return new SearchResponse(results, applied);
    }
}
