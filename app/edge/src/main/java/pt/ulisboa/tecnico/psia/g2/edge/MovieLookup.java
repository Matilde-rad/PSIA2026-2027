package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

// Profiles and people name movies by catalog id; the UI needs titles and posters.
@ApplicationScoped
public class MovieLookup {

    @Inject
    @RestClient
    MoviesClient movies;

    // id -> movie for the ids the catalog has. No call when there is nothing to look up.
    public Uni<Map<String, Movie>> byId(Collection<String> ids) {
        List<String> distinct = ids.stream().distinct().toList();
        if (distinct.isEmpty()) {
            return Uni.createFrom().item(Map.of());
        }
        return movies.lookup(new MoviesClient.LookupRequest(distinct)).map(found -> {
            Map<String, Movie> byId = new LinkedHashMap<>();
            found.forEach(m -> byId.put(m.id(), Movie.from(m)));
            return byId;
        });
    }

    // the movies for these ids, in the same order; ids the catalog does not have are left out
    static List<Movie> inOrder(List<String> ids, Map<String, Movie> byId) {
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }
}
