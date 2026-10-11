package pt.ulisboa.tecnico.psia.g2.users;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/profiles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfileResource {

    @GET
    public List<Profile> list() {
        return Profile.listAll();
    }

    @GET
    @Path("/{userId}")
    public Profile get(@PathParam("userId") UUID userId) {
        Profile profile = Profile.findById(userId);
        if (profile == null) throw new NotFoundException();
        return profile;
    }

    @POST
    @Transactional
    public Response create(Profile profile) {
        if (profile.userId == null) {
            profile.userId = UUID.randomUUID();
        }
        profile.persist();
        return Response.status(Response.Status.CREATED).entity(profile).build();
    }

    @PUT
    @Path("/{userId}")
    @Transactional
    public Profile update(@PathParam("userId") UUID userId, Profile incoming) {
        Profile profile = Profile.findById(userId);
        if (profile == null) throw new NotFoundException();
        profile.displayName = incoming.displayName;
        profile.bio = incoming.bio;
        profile.favoriteGenres = incoming.favoriteGenres;
        profile.favoriteDirectors = incoming.favoriteDirectors;
        profile.favoriteMovies = incoming.favoriteMovies;
        return profile;
    }

    @DELETE
    @Path("/{userId}")
    @Transactional
    public Response delete(@PathParam("userId") UUID userId) {
        boolean deleted = Profile.deleteById(userId);
        if (!deleted) throw new NotFoundException();
        return Response.noContent().build();
    }
}
