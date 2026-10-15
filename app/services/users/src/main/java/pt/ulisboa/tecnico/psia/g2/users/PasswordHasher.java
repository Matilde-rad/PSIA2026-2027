package pt.ulisboa.tecnico.psia.g2.users;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class PasswordHasher {

    private static final int BCRYPT_MAX_BYTES = 72;

    @ConfigProperty(name = "users.bcrypt.cost")
    int cost;

    public String hash(String password) {
        return BcryptUtil.bcryptHash(password, cost);
    }

    public boolean matches(String password, String storedHashOrNull) {
        if (storedHashOrNull == null || password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            return false;
        }
        return BcryptUtil.matches(password, storedHashOrNull);
    }
}
