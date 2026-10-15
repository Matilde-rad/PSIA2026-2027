package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class People {

    @Inject
    MovieLookup movieLookup;

    public Uni<List<PersonView>> views(List<Person> people) {
        List<String> ids = people.stream().flatMap(p -> p.sharedMovies().stream()).toList();
        return movieLookup.byId(ids)
                .map(movies -> people.stream().map(p -> PersonView.of(p, movies)).toList());
    }
}
