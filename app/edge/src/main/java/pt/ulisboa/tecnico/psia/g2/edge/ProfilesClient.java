package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

// Profiles live in the users service, hence the shared configKey.
@Path("/profiles")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "users")
public interface ProfilesClient {

    @GET
    Uni<List<Profile>> list(@QueryParam("genre") String genre, @QueryParam("director") String director);
}
