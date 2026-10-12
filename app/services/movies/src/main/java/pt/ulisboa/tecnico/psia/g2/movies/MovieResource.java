package pt.ulisboa.tecnico.psia.g2.movies;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@Path("/movies")
public class MovieResource {

    static final int DEFAULT_LIMIT = 15;
    static final int MAX_SIMILAR = 200;

    public record SimilarRequest(float[] embedding, Integer limit, Integer minRatings) {
    }

    @Inject
    CatalogRepository catalog;

    @POST
    @Path("/similar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<SimilarMovie> similar(SimilarRequest request) {
        if (request == null || request.embedding() == null || request.embedding().length == 0) {
            return List.of();
        }
        int capped = Math.max(1, Math.min(MAX_SIMILAR, request.limit() == null ? DEFAULT_LIMIT : request.limit()));
        return catalog.similar(request.embedding(), capped, request.minRatings() == null ? 0 : request.minRatings());
    }

    @GET
    @Path("/embeddings")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Integer> embeddings() {
        return Map.of("embedded", catalog.embedded());
    }
}
