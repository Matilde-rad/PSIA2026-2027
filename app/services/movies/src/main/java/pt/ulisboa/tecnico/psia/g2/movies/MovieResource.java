package pt.ulisboa.tecnico.psia.g2.movies;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

// hardcoded response.
@Path("/movies")
public class MovieResource {

    static final int DEFAULT_LIMIT = 15;
    static final int MAX_LIMIT = 100;
    static final int MAX_TITLES = 20;

    @Inject
    CatalogRepository catalog;

    @GET
    @Path("/genres")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> genres() {
        return catalog.genres();
    }

    @GET
    @Path("/candidates")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Candidate> candidates(
            @QueryParam("genres") List<String> genres,
            @QueryParam("yearFrom") Integer yearFrom,
            @QueryParam("yearTo") Integer yearTo,
            @QueryParam("limit") Integer limit) {
        List<String> wanted = genres == null
                ? List.of()
                : genres.stream().filter(g -> g != null && !g.isBlank()).toList();
        int capped = Math.max(1, Math.min(MAX_LIMIT, limit == null ? DEFAULT_LIMIT : limit));
        return catalog.candidates(wanted, yearFrom, yearTo, capped);
    }

    @GET
    @Path("/lookup")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Candidate> lookup(@QueryParam("titles") List<String> titles) {
        if (titles == null) {
            return List.of();
        }
        List<String> keys = titles.stream()
                .map(MovieLensCsv::titleKey)
                .filter(k -> !k.isEmpty())
                .distinct()
                .limit(MAX_TITLES)
                .toList();
        return keys.isEmpty() ? List.of() : catalog.lookup(keys);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Movie get(@PathParam("id") String id) {
        return new Movie(id, "title-" + id, 2026);
    }
}
