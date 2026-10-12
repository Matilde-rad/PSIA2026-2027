package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

// Follows and people live in the users service, hence the shared configKey.
@Path("/social")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "users")
public interface SocialClient {

    @GET
    @Path("/suggestions")
    Uni<List<Person>> suggestions(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);

    @GET
    @Path("/search")
    Uni<List<Person>> search(@QueryParam("q") String q,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);

    @GET
    @Path("/{userId}/stats")
    Uni<SocialStats> stats(@PathParam("userId") String userId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);

    @GET
    @Path("/{userId}/followers")
    Uni<List<Person>> followers(@PathParam("userId") String userId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);

    @GET
    @Path("/{userId}/following")
    Uni<List<Person>> following(@PathParam("userId") String userId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);

    @PUT
    @Path("/following/{userId}")
    Uni<Response> follow(@PathParam("userId") String userId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);

    @DELETE
    @Path("/following/{userId}")
    Uni<Response> unfollow(@PathParam("userId") String userId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);
}
