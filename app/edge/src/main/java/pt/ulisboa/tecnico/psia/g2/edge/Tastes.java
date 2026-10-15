package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

// Builds the caller's Taste from users. A watchlist that cannot be read counts as empty: the
// recommendations then only miss the watched movies, they do not fail.
@ApplicationScoped
public class Tastes {

    private static final Logger LOG = Logger.getLogger(Tastes.class);

    @Inject
    @RestClient
    ProfilesClient profiles;

    @Inject
    @RestClient
    WatchlistClient watchlist;

    // null when the user has no profile yet
    Uni<Taste> of(String userId, String authorization) {
        return profiles.get(userId, authorization).onFailure(Upstream::notFound).recoverWithNull()
                .flatMap(profile -> profile == null ? Uni.createFrom().<Taste>nullItem() : of(profile, authorization));
    }

    Uni<Taste> of(Profile profile, String authorization) {
        return watchlist.list(authorization)
                .onFailure().recoverWithItem(t -> {
                    LOG.warnf("watchlist not read for recommendations error=%s", t.getClass().getSimpleName());
                    return List.of();
                })
                .map(entries -> Taste.of(profile, entries));
    }
}
