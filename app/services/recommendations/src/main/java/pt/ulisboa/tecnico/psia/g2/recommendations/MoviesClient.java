package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.List;

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

    record SimilarRequest(float[] embedding, int limit, int minRatings) {
    }

    record Similar(String id, String title, Integer year, List<String> genres, int ratingCount,
            String posterUrl, String bannerUrl, double similarity) {
    }
}
