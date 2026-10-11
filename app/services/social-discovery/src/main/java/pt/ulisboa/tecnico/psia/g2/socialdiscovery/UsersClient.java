package pt.ulisboa.tecnico.psia.g2.socialdiscovery;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;
import java.util.UUID;

@RegisterRestClient(configKey = "users")
@Path("/profiles")
@Produces(MediaType.APPLICATION_JSON)
public interface UsersClient {

    @GET
    List<Profile> listAll();

    @GET
    @Path("/{userId}")
    Profile getById(@PathParam("userId") UUID userId);
}
