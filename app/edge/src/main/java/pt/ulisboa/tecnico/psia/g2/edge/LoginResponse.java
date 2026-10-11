package pt.ulisboa.tecnico.psia.g2.edge;

public record LoginResponse(String token, String expiresAt, User user) {
}
