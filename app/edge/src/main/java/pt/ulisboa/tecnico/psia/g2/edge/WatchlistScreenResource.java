package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;
import java.util.Map;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/screens/watchlist")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class WatchlistScreenResource {

    static final String TO_WATCH = "to_watch";

    @Inject
    @RestClient
    WatchlistClient watchlist;

    @Inject
    MovieLookup movieLookup;

    // users knows the ids, movies the titles and posters: one lookup for the whole list.
    @GET
    public Uni<WatchlistView> get(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        return watchlist.list(authorization).chain(entries ->
                movieLookup.byId(entries.stream().map(MovieEntry::movieId).toList())
                        .map(movies -> new WatchlistView(
                                items(entries, movies, true), items(entries, movies, false))));
    }

    private static List<WatchlistView.Item> items(List<MovieEntry> entries, Map<String, Movie> movies,
            boolean toWatch) {
        return entries.stream()
                .filter(e -> TO_WATCH.equals(e.status()) == toWatch && movies.containsKey(e.movieId()))
                .map(e -> WatchlistView.Item.of(e, movies.get(e.movieId())))
                .toList();
    }
}
