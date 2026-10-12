package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

// The genres the profile form offers: the ones the movie catalog really has.
@Path("/screens/genres")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class GenresScreenResource {

    @Inject
    @RestClient
    MoviesClient movies;

    @GET
    public Uni<List<String>> list() {
        return movies.genres();
    }
}
