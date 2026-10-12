package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/screens/discover")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class DiscoverScreenResource {

    @Inject
    @RestClient
    ProfilesClient profiles;

    @GET
    public Uni<List<Profile>> list(@QueryParam("genre") String genre, @QueryParam("director") String director,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        return profiles.list(genre, director, authorization);
    }
}
