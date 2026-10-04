package pt.ulisboa.tecnico.psia.g2.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PasswordHasherTest {

    @Inject
    PasswordHasher hasher;

    @Test
    void testProfileUsesLowBcryptCost() {
        String hash = hasher.hash("password1");

        assertTrue(hash.matches("^\\$2[aby]?\\$04\\$.*"), "hash was " + hash.substring(0, 7));
        assertTrue(BcryptUtil.matches("password1", hash));
    }

    @Test
    void productionDefaultCostStays12() throws IOException {
        Properties properties = new Properties();
        Enumeration<URL> candidates = Thread.currentThread().getContextClassLoader()
                .getResources("application.properties");
        while (candidates.hasMoreElements()) {
            Properties candidate = new Properties();
            try (InputStream in = candidates.nextElement().openStream()) {
                candidate.load(in);
            }
            if ("users".equals(candidate.getProperty("quarkus.application.name"))) {
                properties = candidate;
            }
        }
        assertNotNull(properties.getProperty("quarkus.application.name"), "users application.properties not found");

        assertEquals("12", properties.getProperty("users.bcrypt.cost"));
        assertEquals("4", properties.getProperty("%test.users.bcrypt.cost"));
    }
}
