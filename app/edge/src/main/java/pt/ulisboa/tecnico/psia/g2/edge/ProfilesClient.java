package pt.ulisboa.tecnico.psia.g2.edge;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

// Profiles live in the users service, hence the shared configKey.
@Path("/profiles")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "users")
public interface ProfilesClient {

    // 404 when the user has not set up a profile yet
    @GET
    @Path("/{userId}")
    Uni<Profile> get(@PathParam("userId") String userId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);

    // creates the profile on the first save, replaces it afterwards
    @PUT
    @Path("/{userId}")
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<Profile> save(@PathParam("userId") String userId, Profile profile,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);
}
