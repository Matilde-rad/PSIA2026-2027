package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

// Turns the activity users returns into what the UI shows, with one movie lookup for the whole
// list. An activity about a movie the catalog does not have is left out.
@ApplicationScoped
public class Activities {

    @Inject
    MovieLookup movieLookup;

    public Uni<List<ActivityView>> views(List<Activity> activities) {
        return movieLookup.byId(activities.stream().map(Activity::movieId).toList())
                .map(movies -> activities.stream()
                        .filter(a -> movies.containsKey(a.movieId()))
                        .map(a -> new ActivityView(a.userId(), a.username(), a.displayName(), a.status(), a.at(),
                                movies.get(a.movieId())))
                        .toList());
    }
}
