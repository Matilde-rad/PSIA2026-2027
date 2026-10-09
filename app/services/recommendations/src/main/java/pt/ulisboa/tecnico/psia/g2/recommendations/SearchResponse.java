package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.List;

public record SearchResponse(List<Recommendation> results, AppliedFilters appliedFilters) {

    public record AppliedFilters(List<String> genres, Integer yearFrom, Integer yearTo) {
    }
}
