package pt.ulisboa.tecnico.psia.g2.users;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    private static final String USERNAME_UNIQUE_CONSTRAINT = "users_username_key";

    @Inject
    UserRepository users;

    @Inject
    PasswordHasher passwordHasher;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response register(@Valid @NotNull RegisterRequest request, @Context UriInfo uriInfo) {
        String username = request.username().toLowerCase(Locale.ROOT);   // usernames are case-insensitive
        String bio = request.bio() != null && !request.bio().isBlank() ? request.bio().strip() : null;
        Set<Genre> genres = request.favoriteGenres() != null ? request.favoriteGenres() : Set.of();
        String passwordHash = passwordHasher.hash(request.password());
        User user = new User(username, passwordHash, bio, genres);

        try {
            users.persist(user);
            users.flush();
        } catch (ConstraintViolationException e) {
            if (USERNAME_UNIQUE_CONSTRAINT.equals(e.getConstraintName())) {
                throw new WebApplicationException("username already taken", Response.Status.CONFLICT);
            }
            throw e;
        }

        return Response.created(uriInfo.getAbsolutePathBuilder().path(user.getId().toString()).build())
                .entity(UserView.from(user))
                .build();
    }

    @GET
    @Path("/{id}")
    public UserView get(@PathParam("id") String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException();
        }
        return users.findByIdOptional(uuid)
                .map(UserView::from)
                .orElseThrow(NotFoundException::new);
    }
}
