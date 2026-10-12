package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/movies")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "movies")
public interface MoviesClient {

    // every genre in the catalog, alphabetical
    @GET
    @Path("/genres")
    Uni<List<String>> genres();

    // title autocomplete
    @GET
    @Path("/search")
    Uni<List<CatalogMovie>> search(@QueryParam("q") String q, @QueryParam("limit") int limit);

    // the movies with these ids, in the order asked; unknown ids are left out
    @POST
    @Path("/lookup")
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<List<CatalogMovie>> lookup(LookupRequest request);

    record LookupRequest(List<String> ids) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CatalogMovie(String id, String title, Integer year, String posterUrl, String bannerUrl) {
    }
}
