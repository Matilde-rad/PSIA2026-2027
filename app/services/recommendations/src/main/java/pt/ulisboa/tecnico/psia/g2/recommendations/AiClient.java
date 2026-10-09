package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "ai")
public interface AiClient {

    @POST
    @Path("/extract-filters")
    Filters extractFilters(ExtractRequest request);

    @POST
    @Path("/rank")
    RankResponse rank(RankRequest request);

    record ExtractRequest(String text, List<String> allowedGenres) {
    }

    record Filters(List<String> genres, Integer yearFrom, Integer yearTo) {
    }

    record RankRequest(String request, int limit, List<RankCandidate> candidates) {
    }

    record RankCandidate(String id, String title, List<String> genres) {
    }

    record RankResponse(List<Pick> picks) {
    }

    record Pick(String movieId) {
    }
}
