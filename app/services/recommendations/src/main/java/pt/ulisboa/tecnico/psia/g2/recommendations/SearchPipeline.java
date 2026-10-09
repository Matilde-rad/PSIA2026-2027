package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import pt.ulisboa.tecnico.psia.g2.recommendations.SearchResponse.AppliedFilters;

@ApplicationScoped
public class SearchPipeline {

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
        String request = text == null ? "" : text;

        List<String> allowed = movies.genres();

        AiClient.Filters filters = ai.extractFilters(new AiClient.ExtractRequest(request, allowed));
        AppliedFilters applied = new AppliedFilters(filters.genres(), filters.yearFrom(), filters.yearTo());

        List<MoviesClient.Candidate> found = movies.candidates(
                filters.genres(), filters.yearFrom(), filters.yearTo(), candidateCount);
        if (found.isEmpty()) {
            return new SearchResponse(List.of(), applied);
        }

        List<AiClient.RankCandidate> ranked = found.stream()
                .map(c -> new AiClient.RankCandidate(c.id(), c.title(), c.genres()))
                .toList();
        AiClient.RankResponse picks = ai.rank(new AiClient.RankRequest(request, pickCount, ranked));

        Map<String, MoviesClient.Candidate> byId = new LinkedHashMap<>();
        for (MoviesClient.Candidate c : found) {
            byId.put(c.id(), c);
        }

        List<Recommendation> results = new ArrayList<>();
        for (AiClient.Pick pick : picks.picks()) {
            MoviesClient.Candidate candidate = byId.get(pick.movieId());
            if (candidate != null) {
                results.add(new Recommendation(candidate.id(), candidate.title()));
            }
        }
        return new SearchResponse(results, applied);
    }
}
