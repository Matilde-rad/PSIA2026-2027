package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

// Title autocomplete for the favorite movies field of the profile form.
@Path("/screens/movies/search")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class MovieSearchResource {

    static final int SUGGESTIONS = 8;
    static final int MIN_LENGTH = 2;

    @Inject
    @RestClient
    MoviesClient movies;

    @GET
    public Uni<List<Movie>> search(@QueryParam("q") String q) {
        String text = q == null ? "" : q.strip();
        if (text.length() < MIN_LENGTH) {
            return Uni.createFrom().item(List.of());
        }
        return movies.search(text, SUGGESTIONS).map(found -> found.stream().map(Movie::from).toList());
    }
}
