package pt.ulisboa.tecnico.psia.g2.users;

import io.quarkus.security.UnauthorizedException;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

// For other services, not for edge or the UI. There is no user behind these calls, so instead
// of a JWT they carry the shared key (users.internal.key).
@Path("/internal")
@Produces(MediaType.APPLICATION_JSON)
public class InternalResource {

    static final String KEY_HEADER = "X-Internal-Key";

    @ConfigProperty(name = "users.internal.key")
    String internalKey;

    // every profile, for the recommendations agent
    @GET
    @Path("/profiles")
    @PermitAll
    public List<Profile> profiles(@HeaderParam(KEY_HEADER) String key) {
        if (key == null || !MessageDigest.isEqual(bytes(key), bytes(internalKey))) {
            throw new UnauthorizedException();
        }
        return Profile.listAll();
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
