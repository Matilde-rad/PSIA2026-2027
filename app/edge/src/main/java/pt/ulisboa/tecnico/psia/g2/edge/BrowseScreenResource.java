package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/screens/browse")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class BrowseScreenResource {

    // the web app's PAGE_SIZE: a shorter page tells it there is no more
    static final int PAGE = 12;

    @Inject
    @RestClient
    MoviesClient movies;

    @GET
    public Uni<List<Movie>> browse(@QueryParam("q") String q, @QueryParam("genre") List<String> genres,
            @QueryParam("offset") @DefaultValue("0") int offset) {
        return movies.browse(q, genres, offset, PAGE).map(found -> found.stream().map(Movie::from).toList());
    }
}
