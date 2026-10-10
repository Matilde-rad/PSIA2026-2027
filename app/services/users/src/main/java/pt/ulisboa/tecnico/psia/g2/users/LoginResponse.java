package pt.ulisboa.tecnico.psia.g2.users;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt, UserView user) {
}
