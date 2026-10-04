package pt.ulisboa.tecnico.psia.g2.users;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class PasswordHasher {

    @ConfigProperty(name = "users.bcrypt.cost", defaultValue = "12")
    int cost;

    public String hash(String password) {
        return BcryptUtil.bcryptHash(password, cost);
    }
}
