package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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
    Uni<List<String>> genres();

    @GET
    @Path("/search")
    Uni<List<CatalogMovie>> search(@QueryParam("q") String q, @QueryParam("limit") int limit);

    @GET
    @Path("/browse")
    Uni<List<CatalogMovie>> browse(@QueryParam("q") String q, @QueryParam("genre") List<String> genres,
            @QueryParam("offset") int offset, @QueryParam("limit") int limit);

    @POST
    @Path("/lookup")
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<List<CatalogMovie>> lookup(LookupRequest request);

    @GET
    @Path("/{id}")
    Uni<MovieDetails> details(@PathParam("id") String id);

    record LookupRequest(List<String> ids) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CatalogMovie(String id, String title, Integer year, String posterUrl, String bannerUrl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MovieDetails(String id, String title, Integer year, List<String> genres, String posterUrl,
            String bannerUrl, String description, Integer runtimeMinutes, List<String> cast, List<String> directors,
            Double imdbRating) {
    }
}
