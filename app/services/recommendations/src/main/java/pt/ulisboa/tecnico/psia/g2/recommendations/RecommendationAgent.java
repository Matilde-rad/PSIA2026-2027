package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.List;

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
    SearchPipeline pipeline;

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
        int cached = 0;
        for (UserTaste user : all) {
            try {
                List<Recommendation> results = pipeline.search(user.taste()).results();
                if (!results.isEmpty()) {
                    cache.replace(user.id(), results);
                    cached++;
                }
            } catch (RuntimeException e) {
                LOG.warnf("agent user=%s kept cache error=%s", user.id(), e.getClass().getSimpleName());
            }
        }
        LOG.infof("agent run users=%d cached=%d kept=%d ms=%d", all.size(), cached, all.size() - cached,
                (System.nanoTime() - start) / 1_000_000);
    }
}
