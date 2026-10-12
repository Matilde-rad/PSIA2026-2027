package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

// Activity is read from the watchlists in the users service, hence the shared configKey.
@Path("/activity")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "users")
public interface ActivityClient {

    // the people the caller follows, newest first
    @GET
    @Path("/feed")
    Uni<List<Activity>> feed(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);

    @GET
    @Path("/users/{userId}")
    Uni<List<Activity>> of(@PathParam("userId") String userId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);
}
