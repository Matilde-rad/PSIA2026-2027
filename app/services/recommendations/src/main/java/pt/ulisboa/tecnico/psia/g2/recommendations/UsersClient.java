package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/internal")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "users")
public interface UsersClient {

    // Every profile. The agent has no user token; users accepts the shared key instead.
    @GET
    @Path("/profiles")
    List<TasteProfile> profiles(@HeaderParam("X-Internal-Key") String internalKey);
}
