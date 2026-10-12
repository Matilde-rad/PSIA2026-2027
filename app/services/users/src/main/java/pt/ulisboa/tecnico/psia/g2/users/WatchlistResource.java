package pt.ulisboa.tecnico.psia.g2.users;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;

// The caller's own movies: the ones they want to watch and the ones they watched, liked or not.
// A path that is not a movie id matches nothing, so it is a 404.
@Path("/watchlist")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class WatchlistResource {

    static final String MOVIE_ID = "{movieId: [0-9]{1,9}}";

    public record StatusRequest(
            @NotNull @Pattern(regexp = "to_watch|liked|disliked", message = "must be to_watch, liked or disliked")
            String status) {
    }

    @Inject
    WatchlistRepository watchlist;

    @Inject
    JsonWebToken jwt;

    // newest first
    @GET
    public List<MovieEntry> list() {
        return watchlist.list(caller());
    }

    @GET
    @Path(MOVIE_ID)
    public MovieEntry get(@PathParam("movieId") String movieId) {
        return watchlist.find(caller(), movieId).orElseThrow(NotFoundException::new);
    }

    // Adds the movie or changes its status: to_watch, or liked or disliked once watched.
    @PUT
    @Path(MOVIE_ID)
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public MovieEntry save(@PathParam("movieId") String movieId, @Valid @NotNull StatusRequest request) {
        UUID me = caller();
        watchlist.save(me, movieId, request.status());
        return watchlist.find(me, movieId).orElseThrow();
    }

    // removing a movie that is not on the list is not an error
    @DELETE
    @Path(MOVIE_ID)
    @Transactional
    public Response remove(@PathParam("movieId") String movieId) {
        watchlist.remove(caller(), movieId);
        return Response.noContent().build();
    }

    private UUID caller() {
        return UUID.fromString(jwt.getSubject());
    }
}
