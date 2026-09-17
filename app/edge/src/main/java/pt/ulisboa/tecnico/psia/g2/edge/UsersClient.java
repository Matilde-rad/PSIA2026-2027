package pt.ulisboa.tecnico.psia.g2.edge;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "users")
public interface UsersClient {

    @GET
    @Path("/{id}")
    Uni<User> get(@PathParam("id") String id);
}
