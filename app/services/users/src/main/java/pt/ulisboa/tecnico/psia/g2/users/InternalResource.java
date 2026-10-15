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


@Path("/internal")
@Produces(MediaType.APPLICATION_JSON)
public class InternalResource {

    static final String KEY_HEADER = "X-Internal-Key";

    @ConfigProperty(name = "users.internal.key")
    String internalKey;

    @Inject
    WatchlistRepository watchlist;

    @GET
    @Path("/profiles")
    @PermitAll
    public List<Profile> profiles(@HeaderParam(KEY_HEADER) String key) {
        requireKey(key);
        return Profile.listAll();
    }

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
