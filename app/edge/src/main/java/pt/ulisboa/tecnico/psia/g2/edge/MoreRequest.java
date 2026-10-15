package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;
import java.util.Objects;

// A profile sent by the browser is ignored: edge fills in the caller's own taste.
public record MoreRequest(Taste profile, List<String> exclude) {

    public MoreRequest {
        exclude = exclude == null ? List.of() : exclude.stream().filter(Objects::nonNull).toList();
    }
}
