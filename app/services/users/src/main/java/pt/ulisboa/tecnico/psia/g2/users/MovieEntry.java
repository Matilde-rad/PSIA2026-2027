package pt.ulisboa.tecnico.psia.g2.users;

import java.time.Instant;

public record MovieEntry(String movieId, String status, Instant updatedAt) {
}
