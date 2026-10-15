package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    // Every movie every user marked as watched, with whether they liked it.
    @GET
    @Path("/watched")
    List<Watched> watched(@HeaderParam("X-Internal-Key") String internalKey);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Watched(String userId, String movieId, boolean liked) {
    }
}
