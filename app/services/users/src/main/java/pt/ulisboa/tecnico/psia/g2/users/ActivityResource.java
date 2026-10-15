package pt.ulisboa.tecnico.psia.g2.users;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/activity")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class ActivityResource {

    static final int FEED = 40;
    static final int PER_USER = 12;

    @Inject
    WatchlistRepository watchlist;

    @Inject
    UserRepository users;

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("/feed")
    public List<Activity> feed() {
        return watchlist.feed(UUID.fromString(jwt.getSubject()), FEED);
    }

    // anyone logged in can read it, like a profile
    @GET
    @Path("/users/{userId}")
    public List<Activity> of(@PathParam("userId") UUID userId) {
        if (users.findByIdOptional(userId).isEmpty()) {
            throw new NotFoundException();
        }
        return watchlist.of(userId, PER_USER);
    }
}
