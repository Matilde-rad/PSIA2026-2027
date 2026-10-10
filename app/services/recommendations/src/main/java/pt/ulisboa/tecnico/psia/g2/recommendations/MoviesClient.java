package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/movies")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "movies")
public interface MoviesClient {

    @GET
    @Path("/genres")
    List<String> genres();

    @GET
    @Path("/candidates")
    List<Candidate> candidates(@QueryParam("genres") List<String> genres,
            @QueryParam("yearFrom") Integer yearFrom,
            @QueryParam("yearTo") Integer yearTo,
            @QueryParam("limit") int limit);

    @GET
    @Path("/lookup")
    List<Candidate> lookup(@QueryParam("titles") List<String> titles);

    record Candidate(String id, String title, Integer year, List<String> genres, int ratingCount) {
    }
}
