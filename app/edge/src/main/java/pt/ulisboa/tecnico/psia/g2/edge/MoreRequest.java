package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;
import java.util.Objects;

// The browser sends only exclude (ids of the movies it already shows); a profile sent by the
// browser is ignored. edge fills in the caller's own profile before calling recommendations.
public record MoreRequest(Profile profile, List<String> exclude) {

    public MoreRequest {
        exclude = exclude == null ? List.of() : exclude.stream().filter(Objects::nonNull).toList();
    }
}
