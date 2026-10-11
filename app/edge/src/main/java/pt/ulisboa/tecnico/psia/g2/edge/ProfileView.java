package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

public record ProfileView(String id, String username, String createdAt, List<Recommendation> recommendations) {
    
    static ProfileView of(User user, List<Recommendation> recommendations) {
        return new ProfileView(user.id(), user.username(), user.createdAt(), recommendations);
    }
}
