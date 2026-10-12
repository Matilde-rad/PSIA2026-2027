package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

// Profiles name favorite movies by catalog id; the search text needs their titles.
@ApplicationScoped
public class MovieTitles {

    // movies answers at most this many ids per lookup
    static final int CHUNK = 500;

    @Inject
    @RestClient
    MoviesClient movies;

    // id -> title for the ids the catalog has
    public Map<String, String> of(Collection<String> ids) {
        List<String> distinct = new ArrayList<>(ids.stream().distinct().toList());
        Map<String, String> titles = new HashMap<>();
        for (int from = 0; from < distinct.size(); from += CHUNK) {
            List<String> chunk = distinct.subList(from, Math.min(distinct.size(), from + CHUNK));
            movies.lookup(new MoviesClient.LookupRequest(chunk)).forEach(m -> titles.put(m.id(), m.title()));
        }
        return titles;
    }
}
