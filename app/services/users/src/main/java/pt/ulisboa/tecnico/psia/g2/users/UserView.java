package pt.ulisboa.tecnico.psia.g2.users;

import java.time.Instant;

public record UserView(String id, String username, Instant createdAt) {

    public static UserView from(User user) {
        return new UserView(user.getId().toString(), user.getUsername(), user.getCreatedAt());
    }
}
