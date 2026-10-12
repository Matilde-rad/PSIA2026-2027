package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            all = users.profiles(internalKey);
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
        // Users with the same taste get the same picks, so each distinct taste is searched once.
        // A profile that says nothing about taste is not searched at all.
        Map<String, TasteProfile> tastes = new LinkedHashMap<>();
        for (TasteProfile profile : all) {
            String taste = profile.text(titles);
            if (!taste.isEmpty()) {
                tastes.putIfAbsent(taste, profile);
            }
        }
        Map<String, List<Recommendation>> found = searchEach(tastes);
        int cached = 0;
        for (TasteProfile profile : all) {
            String taste = profile.text(titles);
            if (taste.isEmpty()) {
                continue;
            }
            List<Recommendation> results = found.get(taste);
            if (results == null) {
                LOG.warnf("agent user=%s kept cache", profile.userId());
            } else if (!results.isEmpty()) {
                cache.replace(profile.userId(), results);
                cached++;
            }
        }
        LOG.infof("agent run users=%d tastes=%d cached=%d kept=%d ms=%d", all.size(), tastes.size(), cached,
                all.size() - cached, (System.nanoTime() - start) / 1_000_000);
    }

    // One user, right now: called when a profile is saved, so the picks do not wait for the next run.
    // Throws SemanticSearch.AiUnavailableException when the AI cannot answer; the cache is then kept.
    public List<Recommendation> refresh(String userId, TasteProfile profile) {
        if (profile == null) {
            return cache.read(userId);
        }
        String taste;
        try {
            taste = profile.text(movieTitles.of(profile.favoriteMovies()));
        } catch (RuntimeException e) {
            throw new SemanticSearch.AiUnavailableException(e);
        }
        if (taste.isEmpty()) {
            return cache.read(userId);
        }
        List<Recommendation> results = semantic.search(taste, profile.favoriteMovies()).results();
        if (!results.isEmpty()) {
            cache.replace(userId, results);
        }
        return results;
    }

    private Map<String, List<Recommendation>> searchEach(Map<String, TasteProfile> tastes) {
        Map<String, List<Recommendation>> found = new LinkedHashMap<>();
        tastes.forEach((taste, profile) -> {
            try {
                found.put(taste, semantic.search(taste, profile.favoriteMovies()).results());
            } catch (RuntimeException e) {
                LOG.warnf("agent search failed error=%s", e.getClass().getSimpleName());
            }
        });
        return found;
    }
}
