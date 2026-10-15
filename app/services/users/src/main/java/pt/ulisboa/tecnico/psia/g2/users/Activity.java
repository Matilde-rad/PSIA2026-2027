package pt.ulisboa.tecnico.psia.g2.users;

import java.time.Instant;

public record Activity(String userId, String username, String displayName, String movieId, String status,
        Instant at) {
}
