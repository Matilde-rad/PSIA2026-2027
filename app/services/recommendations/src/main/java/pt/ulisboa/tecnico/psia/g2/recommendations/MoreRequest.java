package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.List;
import java.util.Objects;

public record MoreRequest(TasteProfile profile, List<String> exclude) {

    public MoreRequest {
        exclude = exclude == null ? List.of() : exclude.stream().filter(Objects::nonNull).toList();
    }
}
