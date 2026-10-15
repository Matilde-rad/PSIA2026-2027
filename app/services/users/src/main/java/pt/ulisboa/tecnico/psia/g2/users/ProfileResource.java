package pt.ulisboa.tecnico.psia.g2.users;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/profiles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class ProfileResource {

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("/{userId}")
    public Profile get(@PathParam("userId") UUID userId) {
        Profile profile = Profile.findById(userId);
        if (profile == null) throw new NotFoundException();
        return profile;
    }

    // Creates the caller's own profile; the id comes from the token, never from the body.
    @POST
    @Transactional
    public Response create(@Valid @NotNull Profile profile) {
        UUID caller = caller();
        if (Profile.findById(caller) != null) {
            throw new WebApplicationException("profile already exists", Response.Status.CONFLICT);
        }
        profile.userId = caller;
        profile.favoriteGenres = orEmpty(profile.favoriteGenres);
        profile.favoriteDirectors = orEmpty(profile.favoriteDirectors);
        profile.favoriteMovies = orEmpty(profile.favoriteMovies);
        profile.persist();
        return Response.status(Response.Status.CREATED).entity(profile).build();
    }

    // Creates the profile on the first save, replaces it afterwards.
    @PUT
    @Path("/{userId}")
    @Transactional
    public Profile save(@PathParam("userId") UUID userId, @Valid @NotNull Profile incoming) {
        requireOwner(userId);
        Profile profile = Profile.findById(userId);
        if (profile == null) {
            profile = new Profile();
            profile.userId = userId;
        }
        profile.displayName = incoming.displayName;
        profile.bio = incoming.bio;
        profile.favoriteGenres = orEmpty(incoming.favoriteGenres);
        profile.favoriteDirectors = orEmpty(incoming.favoriteDirectors);
        profile.favoriteMovies = orEmpty(incoming.favoriteMovies);
        profile.persist();
        return profile;
    }

    @DELETE
    @Path("/{userId}")
    @Transactional
    public Response delete(@PathParam("userId") UUID userId) {
        requireOwner(userId);
        boolean deleted = Profile.deleteById(userId);
        if (!deleted) throw new NotFoundException();
        return Response.noContent().build();
    }

    private UUID caller() {
        return UUID.fromString(jwt.getSubject());
    }

    private void requireOwner(UUID userId) {
        if (!userId.equals(caller())) {
            throw new ForbiddenException("not your profile");
        }
    }

    private static List<String> orEmpty(List<String> list) {
        return list != null ? list : new ArrayList<>();
    }
}
