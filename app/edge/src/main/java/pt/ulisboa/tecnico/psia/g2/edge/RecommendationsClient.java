package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/recommendations")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "recommendations")
public interface RecommendationsClient {

    @GET
    @Path("/users/{userId}")
    Uni<List<Recommendation>> forUser(@PathParam("userId") String userId);
}
