package pt.ulisboa.tecnico.psia.g2.movies;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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
    // more than the catalog's 19 genres, so a real request is never cut
    static final int MAX_BROWSE_GENRES = 20;
    // Postgres sorts offset + limit rows to skip the offset, so the offset is bounded
    static final int MAX_BROWSE_OFFSET = 10_000;

    public record LookupRequest(List<String> ids) {
    }

    public record SimilarRequest(float[] embedding, Integer limit, Integer minRatings) {
    }

    @Inject
    CatalogRepository catalog;

    @Inject
    MovieMetaRepository metas;

    @Inject
    Cinemeta cinemeta;

    @GET
    @Path("/{id: [0-9]{1,9}}")
    @Produces(MediaType.APPLICATION_JSON)
    public MovieDetails details(@PathParam("id") int id) {
        MovieSummary movie = catalog.byIds(List.of(id)).stream().findFirst().orElseThrow(NotFoundException::new);
        MovieMeta meta = metas.find(id).orElseGet(() -> fetchMeta(id));
        return MovieDetails.of(movie, meta);
    }

    private MovieMeta fetchMeta(int id) {
        String imdbId = catalog.imdbId(id);
        if (imdbId == null) {
            return null;
        }
        MovieMeta meta = cinemeta.fetch(imdbId).orElse(null);
        if (meta != null) {
            metas.store(id, meta);
        }
        return meta;
    }

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
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public List<MovieSummary> search(@QueryParam("q") String q, @QueryParam("limit") Integer limit) {
        return catalog.search(q, limit(limit));
    }

    @GET
    @Path("/browse")
    @Produces(MediaType.APPLICATION_JSON)
    public List<MovieSummary> browse(@QueryParam("q") String q, @QueryParam("genre") List<String> genres,
            @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit) {
        int skip = offset == null ? 0 : Math.max(0, offset);
        if (skip > MAX_BROWSE_OFFSET) {
            return List.of();
        }
        List<String> picked = genres == null ? List.of() : genres.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(g -> !g.isEmpty())
                .distinct()
                .limit(MAX_BROWSE_GENRES)
                .toList();
        return catalog.browse(q, picked, skip, limit(limit));
    }

    private static int limit(Integer limit) {
        return Math.max(1, Math.min(MAX_SEARCH_LIMIT, limit == null ? DEFAULT_SEARCH_LIMIT : limit));
    }

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

    @POST
    @Path("/vectors")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<MovieVector> vectors(LookupRequest request) {
        if (request == null || request.ids() == null) {
            return List.of();
        }
        return catalog.vectors(request.ids().stream().limit(MAX_LOOKUP).map(MovieResource::id).filter(Objects::nonNull).toList());
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
