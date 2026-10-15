package pt.ulisboa.tecnico.psia.g2.users;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/social")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class SocialResource {

    static final int SUGGESTIONS = 12;
    static final int SEARCH_RESULTS = 20;
    static final int LIST_LIMIT = 200;
    static final int MIN_SEARCH_LENGTH = 2;

    @Inject
    SocialRepository social;

    @Inject
    UserRepository users;

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("/suggestions")
    public List<Person> suggestions() {
        return social.suggestions(caller(), SUGGESTIONS);
    }

    @GET
    @Path("/search")
    public List<Person> search(@QueryParam("q") String q) {
        String text = q == null ? "" : q.strip();
        if (text.length() < MIN_SEARCH_LENGTH) {
            return List.of();
        }
        return social.search(caller(), text, SEARCH_RESULTS);
    }

    @GET
    @Path("/{userId}/stats")
    public SocialStats stats(@PathParam("userId") UUID userId) {
        return social.stats(caller(), existing(userId));
    }

    @GET
    @Path("/{userId}/followers")
    public List<Person> followers(@PathParam("userId") UUID userId) {
        return social.followers(caller(), existing(userId), LIST_LIMIT);
    }

    @GET
    @Path("/{userId}/following")
    public List<Person> following(@PathParam("userId") UUID userId) {
        return social.following(caller(), existing(userId), LIST_LIMIT);
    }

    @PUT
    @Path("/following/{userId}")
    @Transactional
    public Response follow(@PathParam("userId") UUID userId) {
        UUID me = caller();
        if (me.equals(userId)) {
            throw new BadRequestException("you cannot follow yourself");
        }
        social.follow(me, existing(userId));
        return Response.noContent().build();
    }

    @DELETE
    @Path("/following/{userId}")
    @Transactional
    public Response unfollow(@PathParam("userId") UUID userId) {
        social.unfollow(caller(), userId);
        return Response.noContent().build();
    }

    private UUID caller() {
        return UUID.fromString(jwt.getSubject());
    }

    private UUID existing(UUID userId) {
        if (users.findByIdOptional(userId).isEmpty()) {
            throw new NotFoundException();
        }
        return userId;
    }
}
