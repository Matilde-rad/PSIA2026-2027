package pt.ulisboa.tecnico.psia.g2.users;

import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/profiles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfileResource {

    // Browse other users' profiles. Case-insensitive substring match against the
    // JSON arrays, done in Postgres.
    @GET
    @SuppressWarnings("unchecked")
    public List<Profile> list(@QueryParam("genre") String genre, @QueryParam("director") String director) {
        StringBuilder sql = new StringBuilder("select p.* from profiles p where true");
        Map<String, String> params = new HashMap<>();
        if (genre != null && !genre.isBlank()) {
            sql.append(matches("favoriteGenres", "genre"));
            params.put("genre", genre);
        }
        if (director != null && !director.isBlank()) {
            sql.append(matches("favoriteDirectors", "director"));
            params.put("director", director);
        }
        Query query = Profile.getEntityManager().createNativeQuery(sql.toString(), Profile.class);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    private static String matches(String column, String param) {
        return " and exists (select 1 from jsonb_array_elements_text(p." + column + ") e"
                + " where position(lower(:" + param + ") in lower(e)) > 0)";
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
