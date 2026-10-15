package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RecommendationAgent {

    private static final Logger LOG = Logger.getLogger(RecommendationAgent.class);

    @Inject
    @RestClient
    UsersClient users;

    @Inject
    SemanticSearch semantic;

    @Inject
    RecommendationCache cache;

    @Inject
    MovieTitles movieTitles;

    @ConfigProperty(name = "recs.users.internal-key")
    String internalKey;

    @Scheduled(every = "{recs.agent.every}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void refreshAll() {
        long start = System.nanoTime();
        List<TasteProfile> all;
        try {
            all = withWatched(users.profiles(internalKey), users.watched(internalKey));
        } catch (RuntimeException e) {
            LOG.warnf("agent run skipped users unavailable error=%s", e.getClass().getSimpleName());
            return;
        }
        Map<String, String> titles;
        try {
            titles = movieTitles.of(all.stream().flatMap(p -> p.favoriteMovies().stream()).toList());
        } catch (RuntimeException e) {
            LOG.warnf("agent run skipped movies unavailable error=%s", e.getClass().getSimpleName());
            return;
        }
        // Users with the same taste get the same picks, so each distinct query is searched once.
        // A profile that says nothing about taste is not searched at all.
        Map<TasteProfile, SemanticSearch.Query> queries = new LinkedHashMap<>();
        for (TasteProfile profile : all) {
            SemanticSearch.Query query = profile.query(titles);
            if (!query.isEmpty()) {
                queries.put(profile, query);
            }
        }
        Set<SemanticSearch.Query> distinct = new LinkedHashSet<>(queries.values());
        Map<SemanticSearch.Query, List<Recommendation>> found = searchEach(distinct);
        int cached = 0;
        for (Map.Entry<TasteProfile, SemanticSearch.Query> entry : queries.entrySet()) {
            String userId = entry.getKey().userId();
            List<Recommendation> results = found.get(entry.getValue());
            if (results == null) {
                LOG.warnf("agent user=%s kept cache", userId);
            } else if (!results.isEmpty()) {
                cache.replace(userId, results);
                cached++;
            }
        }
        LOG.infof("agent run users=%d tastes=%d cached=%d kept=%d ms=%d", all.size(), distinct.size(), cached,
                all.size() - cached, (System.nanoTime() - start) / 1_000_000);
    }

    // each profile with the movies its user marked as watched
    static List<TasteProfile> withWatched(List<TasteProfile> profiles, List<UsersClient.Watched> watched) {
        Map<String, List<UsersClient.Watched>> byUser = watched.stream()
                .collect(Collectors.groupingBy(UsersClient.Watched::userId));
        return profiles.stream().map(p -> {
            List<UsersClient.Watched> mine = byUser.getOrDefault(p.userId(), List.of());
            return p.withWatched(
                    mine.stream().filter(UsersClient.Watched::liked).map(UsersClient.Watched::movieId).toList(),
                    mine.stream().filter(w -> !w.liked()).map(UsersClient.Watched::movieId).toList());
        }).toList();
    }

    // One user, right now: called when a profile is saved or a movie is marked, so the picks do not
    // wait for the next run. Throws SemanticSearch.AiUnavailableException when the AI cannot answer;
    // the cache is then kept.
    public List<Recommendation> refresh(String userId, TasteProfile profile) {
        if (profile == null) {
            return cache.read(userId);
        }
        SemanticSearch.Query query = query(profile);
        if (query.isEmpty()) {
            return cache.read(userId);
        }
        List<Recommendation> results = semantic.recommend(query);
        if (!results.isEmpty()) {
            cache.replace(userId, results);
        }
        return results;
    }

    // For you's Refresh and Get more. Never cached, so a reload shows the cached set again.
    public List<Recommendation> more(TasteProfile profile, Collection<String> exclude) {
        if (profile == null) {
            return List.of();
        }
        SemanticSearch.Query query = query(profile);
        return query.isEmpty() ? List.of() : semantic.recommend(query.excluding(exclude));
    }

    private SemanticSearch.Query query(TasteProfile profile) {
        try {
            return profile.query(movieTitles.of(profile.favoriteMovies()));
        } catch (RuntimeException e) {
            throw new SemanticSearch.AiUnavailableException(e);
        }
    }

    private Map<SemanticSearch.Query, List<Recommendation>> searchEach(Set<SemanticSearch.Query> queries) {
        Map<SemanticSearch.Query, List<Recommendation>> found = new LinkedHashMap<>();
        for (SemanticSearch.Query query : queries) {
            try {
                found.put(query, semantic.recommend(query));
            } catch (RuntimeException e) {
                LOG.warnf("agent search failed error=%s", e.getClass().getSimpleName());
            }
        }
        return found;
    }
}
