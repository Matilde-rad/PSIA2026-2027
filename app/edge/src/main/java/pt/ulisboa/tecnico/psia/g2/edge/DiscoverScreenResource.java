package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/screens/discover")
public class DiscoverScreenResource {

    @Inject
    @RestClient
    ProfilesClient profiles;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<Profile>> list(@QueryParam("genre") String genre, @QueryParam("director") String director) {
        return profiles.list(genre, director);
    }
}
