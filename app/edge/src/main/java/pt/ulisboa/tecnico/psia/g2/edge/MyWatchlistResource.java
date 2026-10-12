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
import org.eclipse.microprofile.rest.client.inject.RestClient;

// The caller puts a movie on their watchlist, says they watched it and liked it or not, or takes
// it off. users checks the status; its 400 comes back unchanged.
@Path("/me/watchlist")
@Authenticated
public class MyWatchlistResource {

    @Inject
    @RestClient
    WatchlistClient watchlist;

    // body: {"status": "to_watch" | "liked" | "disliked"}
    @PUT
    @Path("/{movieId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MovieEntry> save(@PathParam("movieId") String movieId, StatusRequest request,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        return watchlist.save(MovieIds.valid(movieId), request, authorization);
    }

    @DELETE
    @Path("/{movieId}")
    public Uni<Response> remove(@PathParam("movieId") String movieId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        return watchlist.remove(MovieIds.valid(movieId), authorization).map(r -> Response.noContent().build());
    }
}
