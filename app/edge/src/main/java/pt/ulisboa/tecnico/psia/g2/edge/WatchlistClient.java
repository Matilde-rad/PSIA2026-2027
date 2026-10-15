package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/watchlist")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "users")
public interface WatchlistClient {

    @GET
    Uni<List<MovieEntry>> list(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);

    @GET
    @Path("/{movieId}")
    Uni<MovieEntry> get(@PathParam("movieId") String movieId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);

    @PUT
    @Path("/{movieId}")
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<MovieEntry> save(@PathParam("movieId") String movieId, StatusRequest request,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);

    @DELETE
    @Path("/{movieId}")
    Uni<Response> remove(@PathParam("movieId") String movieId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);
}
