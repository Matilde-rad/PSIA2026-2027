package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

// edge type for the UI to consume.
public record ProfileView(String userId, String displayName, List<Recommendation> recommendations) {
}
