package pt.ulisboa.tecnico.psia.g2.edge;

import io.smallrye.jwt.build.Jwt;
import java.security.KeyPairGenerator;
import java.time.Instant;

final class Tokens {

    private Tokens() {
    }

    static String valid(String sub) {
        return Jwt.issuer("g2-users").subject(sub).expiresIn(300).sign("test-private.pem");
    }

    static String expired(String sub) {
        Instant now = Instant.now();
        return Jwt.issuer("g2-users").subject(sub)
                .issuedAt(now.minusSeconds(4200))
                .expiresAt(now.minusSeconds(600))
                .sign("test-private.pem");
    }

    static String wrongKey(String sub) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return Jwt.issuer("g2-users").subject(sub).expiresIn(300)
                .sign(generator.generateKeyPair().getPrivate());
    }
}
