package pt.ulisboa.tecnico.psia.g2.users;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TokenIssuer {

    @ConfigProperty(name = "smallrye.jwt.new-token.lifespan", defaultValue = "3600")
    long lifespanSeconds;

    public record IssuedToken(String token, Instant expiresAt) {
    }

    public IssuedToken issue(User user) {
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plusSeconds(lifespanSeconds);

        String token = Jwt.subject(user.getId().toString())
                .upn(user.getUsername())
                .groups(Set.of("user"))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .sign();
        return new IssuedToken(token, expiresAt);
    }
}
