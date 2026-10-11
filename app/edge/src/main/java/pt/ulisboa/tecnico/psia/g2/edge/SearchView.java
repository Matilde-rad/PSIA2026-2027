package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

public record SearchView(List<Recommendation> results, AppliedFilters appliedFilters) {

    public record AppliedFilters(List<String> genres, Integer yearFrom, Integer yearTo) {
    }
}
