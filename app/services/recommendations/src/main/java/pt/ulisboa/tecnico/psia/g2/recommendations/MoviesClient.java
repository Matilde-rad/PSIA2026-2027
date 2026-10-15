package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/movies")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "movies")
public interface MoviesClient {

    @POST
    @Path("/similar")
    @Consumes(MediaType.APPLICATION_JSON)
    List<Similar> similar(SimilarRequest request);

    @POST
    @Path("/lookup")
    @Consumes(MediaType.APPLICATION_JSON)
    List<Movie> lookup(LookupRequest request);

    @POST
    @Path("/vectors")
    @Consumes(MediaType.APPLICATION_JSON)
    List<Vector> vectors(LookupRequest request);

    record Vector(String id, float[] embedding) {
    }

    record LookupRequest(List<String> ids) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Movie(String id, String title) {
    }

    record SimilarRequest(float[] embedding, int limit, int minRatings) {
    }

    record Similar(String id, String title, Integer year, List<String> genres, int ratingCount,
            String posterUrl, String bannerUrl, double similarity) {
    }
}
