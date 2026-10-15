package pt.ulisboa.tecnico.psia.g2.edge;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

// Each change has the caller's recommendations recomputed in the background, so Home drops a
// watched movie without waiting for the agent; the answer does not wait for that.
@Path("/me/watchlist")
@Authenticated
public class MyWatchlistResource {

    private static final Logger LOG = Logger.getLogger(MyWatchlistResource.class);

    @Inject
    @RestClient
    WatchlistClient watchlist;

    @Inject
    @RestClient
    RecommendationsClient recommendations;

    @Inject
    Tastes tastes;

    @Inject
    JsonWebToken jwt;

    @PUT
    @Path("/{movieId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MovieEntry> save(@PathParam("movieId") String movieId, StatusRequest request,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        String userId = jwt.getSubject();
        return watchlist.save(MovieIds.valid(movieId), request, authorization)
                .invoke(() -> refreshInBackground(userId, authorization));
    }

    @DELETE
    @Path("/{movieId}")
    public Uni<Response> remove(@PathParam("movieId") String movieId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        String userId = jwt.getSubject();
        return watchlist.remove(MovieIds.valid(movieId), authorization)
                .invoke(() -> refreshInBackground(userId, authorization))
                .map(r -> Response.noContent().build());
    }

    private void refreshInBackground(String userId, String authorization) {
        Uni.createFrom().deferred(() -> tastes.of(userId, authorization))
                .flatMap(taste -> taste == null
                        ? Uni.createFrom().<List<Recommendation>>nullItem()
                        : recommendations.refresh(userId, taste, authorization))
                .subscribe().with(
                        picks -> {
                        },
                        t -> LOG.warnf("recommendations not refreshed after watchlist change error=%s",
                                t.getClass().getSimpleName()));
    }
}
