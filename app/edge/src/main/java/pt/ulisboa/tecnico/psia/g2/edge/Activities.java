package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
