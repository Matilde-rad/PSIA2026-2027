package pt.ulisboa.tecnico.psia.g2.movies;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Path("/movies")
public class MovieResource {

    static final int DEFAULT_LIMIT = 15;
    static final int MAX_SIMILAR = 200;
    static final int DEFAULT_SEARCH_LIMIT = 8;
    static final int MAX_SEARCH_LIMIT = 20;
    static final int MAX_LOOKUP = 500;

    public record LookupRequest(List<String> ids) {
    }

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

    // title autocomplete, e.g. /movies/search?q=par
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public List<MovieSummary> search(@QueryParam("q") String q, @QueryParam("limit") Integer limit) {
        int capped = Math.max(1, Math.min(MAX_SEARCH_LIMIT, limit == null ? DEFAULT_SEARCH_LIMIT : limit));
        return catalog.search(q, capped);
    }

    // the movies with the given ids, in the order asked
    @POST
    @Path("/lookup")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<MovieSummary> lookup(LookupRequest request) {
        if (request == null || request.ids() == null) {
            return List.of();
        }
        return catalog.byIds(request.ids().stream().limit(MAX_LOOKUP).map(MovieResource::id).filter(Objects::nonNull).toList());
    }

    private static Integer id(String text) {
        try {
            return text == null ? null : Integer.valueOf(text.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @GET
    @Path("/genres")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> genres() {
        return catalog.genres();
    }

    @GET
    @Path("/embeddings")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Integer> embeddings() {
        return Map.of("embedded", catalog.embedded());
    }
}
