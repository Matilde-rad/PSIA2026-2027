package pt.ulisboa.tecnico.psia.g2.users;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PasswordHasherTest {

    @Inject
    PasswordHasher hasher;

    // tests use bcrypt cost 4 so they run faster, prod uses 12
    @Test
    void testProfileUsesLowBcryptCost() {
        assertTrue(hasher.hash("password1").startsWith("$2a$04$"));
    }

    @Test
    void matchesAcceptsOnlyTheRealPassword() {
        String hash = hasher.hash("password1");

        assertTrue(hasher.matches("password1", hash));
        assertFalse(hasher.matches("password2", hash));
        assertFalse(hasher.matches("password1", null));
    }

    // bcrypt ignores everything after 72 bytes, so a longer password must never match
    @Test
    void matchesRejectsPasswordsOver72Bytes() {
        String p72 = "a".repeat(72);
        String hash = hasher.hash(p72);

        assertTrue(hasher.matches(p72, hash));
        assertFalse(hasher.matches(p72 + "b", hash));
    }
}
