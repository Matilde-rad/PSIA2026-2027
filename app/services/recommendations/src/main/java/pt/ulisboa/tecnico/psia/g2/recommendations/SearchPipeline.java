package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        List<MoviesClient.Candidate> suggested = filters.suggestions() == null || filters.suggestions().isEmpty()
                ? List.of()
                : movies.lookup(filters.suggestions());
        // A request with no genre and no year ("movies in the water") would be padded with the
        // most-rated films of all time, which have nothing to do with it: use only the suggestions.
        boolean themeOnly = (filters.genres() == null || filters.genres().isEmpty())
                && filters.yearFrom() == null && filters.yearTo() == null;
        List<MoviesClient.Candidate> popular = themeOnly && !suggested.isEmpty()
                ? List.of()
                : movies.candidates(filters.genres(), filters.yearFrom(), filters.yearTo(), candidateCount);

        Set<String> seen = new HashSet<>();
        List<MoviesClient.Candidate> found = Stream.concat(suggested.stream().filter(c -> fits(c, filters)), popular.stream())
                .filter(c -> seen.add(c.id()))
                .limit(candidateCount)
                .toList();
        if (found.isEmpty()) {
            return new SearchResponse(List.of(), applied);
        }

        List<AiClient.RankCandidate> rankCandidates = found.stream()
                .map(c -> new AiClient.RankCandidate(c.id(), c.title(), c.genres()))
                .toList();
        AiClient.RankResponse picks = ai.rank(new AiClient.RankRequest(text, pickCount, rankCandidates));

        Map<String, MoviesClient.Candidate> byId =
                found.stream().collect(Collectors.toMap(MoviesClient.Candidate::id, Function.identity()));
        List<Recommendation> results = picks.picks().stream()
                .map(p -> byId.get(p.movieId()))
                .filter(Objects::nonNull)
                .map(c -> new Recommendation(c.id(), c.title(), c.posterUrl(), c.bannerUrl()))
                .toList();
        if (results.isEmpty()) {
            throw new AiUnavailableException(null);
        }
        return new SearchResponse(results, applied);
    }

    private static boolean fits(MoviesClient.Candidate c, AiClient.Filters f) {
        return (f.genres() == null || f.genres().isEmpty() || f.genres().stream().anyMatch(c.genres()::contains))
                && (f.yearFrom() == null || (c.year() != null && c.year() >= f.yearFrom()))
                && (f.yearTo() == null || (c.year() != null && c.year() <= f.yearTo()));
    }
}
