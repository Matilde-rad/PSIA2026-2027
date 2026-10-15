package pt.ulisboa.tecnico.psia.g2.edge;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/screens/movie")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class MovieScreenResource {

    @Inject
    @RestClient
    MoviesClient movies;

    @Inject
    @RestClient
    WatchlistClient watchlist;

    @GET
    @Path("/{id}")
    public Uni<MovieView> get(@PathParam("id") String id,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        String movieId = MovieIds.valid(id);
        return Uni.combine().all()
                .unis(movies.details(movieId), entryOrNull(movieId, authorization))
                .with(MovieView::of);
    }

    // Most movies are on nobody's list, so that 404 is not an error here.
    private Uni<MovieEntry> entryOrNull(String movieId, String authorization) {
        return watchlist.get(movieId, authorization).onFailure(Upstream::notFound).recoverWithNull();
    }
}
