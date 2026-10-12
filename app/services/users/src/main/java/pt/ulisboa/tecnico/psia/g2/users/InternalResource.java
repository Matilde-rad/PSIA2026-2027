package pt.ulisboa.tecnico.psia.g2.users;

import io.quarkus.security.UnauthorizedException;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
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

    @Inject
    WatchlistRepository watchlist;

    // every profile, for the recommendations agent
    @GET
    @Path("/profiles")
    @PermitAll
    public List<Profile> profiles(@HeaderParam(KEY_HEADER) String key) {
        requireKey(key);
        return Profile.listAll();
    }

    // Every movie every user watched, with whether they liked it. Nothing reads it yet: it is
    // here for the recommendations agent to learn from.
    @GET
    @Path("/watched")
    @PermitAll
    public List<WatchedMovie> watched(@HeaderParam(KEY_HEADER) String key) {
        requireKey(key);
        return watchlist.watched();
    }

    private void requireKey(String key) {
        if (key == null || !MessageDigest.isEqual(bytes(key), bytes(internalKey))) {
            throw new UnauthorizedException();
        }
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
