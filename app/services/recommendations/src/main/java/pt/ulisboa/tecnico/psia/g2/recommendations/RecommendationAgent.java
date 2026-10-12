package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import pt.ulisboa.tecnico.psia.g2.recommendations.UsersClient.UserTaste;

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

    @Scheduled(every = "{recs.agent.every}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void refreshAll() {
        long start = System.nanoTime();
        List<UserTaste> all;
        try {
            all = users.list();
        } catch (RuntimeException e) {
            LOG.warnf("agent run skipped users unavailable error=%s", e.getClass().getSimpleName());
            return;
        }
        // Users with the same taste get the same picks, so each distinct taste is searched once.
        List<String> tastes = all.stream().map(RecommendationAgent::taste).distinct().toList();
        Map<String, List<Recommendation>> found = searchEach(tastes);
        int cached = 0;
        for (UserTaste user : all) {
            List<Recommendation> results = found.get(taste(user));
            if (results == null) {
                LOG.warnf("agent user=%s kept cache", user.id());
            } else if (!results.isEmpty()) {
                cache.replace(user.id(), results);
                cached++;
            }
        }
        LOG.infof("agent run users=%d tastes=%d cached=%d kept=%d ms=%d", all.size(), tastes.size(), cached,
                all.size() - cached, (System.nanoTime() - start) / 1_000_000);
    }

    private Map<String, List<Recommendation>> searchEach(List<String> tastes) {
        Map<String, List<Recommendation>> found = new HashMap<>();
        for (String taste : tastes) {
            try {
                found.put(taste, semantic.search(taste).results());
            } catch (RuntimeException e) {
                LOG.warnf("agent search failed error=%s", e.getClass().getSimpleName());
            }
        }
        return found;
    }

    private static String taste(UserTaste user) {
        return Objects.requireNonNullElse(user.taste(), "");
    }
}
