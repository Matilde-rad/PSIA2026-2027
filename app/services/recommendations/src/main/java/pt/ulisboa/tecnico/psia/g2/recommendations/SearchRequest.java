package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.List;
import java.util.Objects;

public record SearchRequest(String text, List<String> exclude) {

    public SearchRequest {
        exclude = exclude == null ? List.of() : exclude.stream().filter(Objects::nonNull).toList();
    }
}
