package pt.ulisboa.tecnico.psia.g2.edge;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/screens/profile")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class ProfileScreenResource {

    @Inject
    @RestClient
    UsersClient users;

    @Inject
    @RestClient
    ProfilesClient profiles;

    @Inject
    @RestClient
    SocialClient social;

    @Inject
    @RestClient
    ActivityClient activity;

    @Inject
    @RestClient
    RecommendationsClient recommendations;

    @Inject
    MovieLookup movieLookup;

    @Inject
    People people;

    @Inject
    Activities activities;

    @Inject
    JsonWebToken jwt;

    private record Parts(User user, Profile profile, SocialStats stats, List<Recommendation> recommendations) {
    }

    @GET
    @Path("/{id}")
    public Uni<ProfileView> get(@PathParam("id") String id,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        String userId = uuid(id);
        boolean own = userId.equals(uuid(jwt.getSubject()));

        Uni<List<Recommendation>> recs = own
                ? recommendations.forUser(userId, authorization)
                : Uni.createFrom().item(List.of());
        return Uni.combine().all()
                .unis(users.get(userId, authorization), profileOrNull(userId, authorization),
                        social.stats(userId, authorization), recs)
                .with(Parts::new)
                .chain(this::withFavoriteMovies);
    }

    @GET
    @Path("/{id}/followers")
    public Uni<List<PersonView>> followers(@PathParam("id") String id,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        return social.followers(uuid(id), authorization).chain(people::views);
    }

    @GET
    @Path("/{id}/following")
    public Uni<List<PersonView>> following(@PathParam("id") String id,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        return social.following(uuid(id), authorization).chain(people::views);
    }

    // what this user did with movies lately, newest first
    @GET
    @Path("/{id}/activity")
    public Uni<List<ActivityView>> activity(@PathParam("id") String id,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        return activity.of(uuid(id), authorization).chain(activities::views);
    }

    // The profile stores movie ids; the screen shows titles and posters. If movies cannot answer
    // the screen fails rather than showing no favorites: the edit form is filled from this list,
    // and saving an empty one would erase them.
    private Uni<ProfileView> withFavoriteMovies(Parts parts) {
        List<String> ids = parts.profile() == null ? List.of() : parts.profile().favoriteMovies();
        return movieLookup.byId(ids).map(movies -> new ProfileView(
                parts.user().id(), parts.user().username(), parts.user().createdAt(), parts.profile(),
                MovieLookup.inOrder(ids, movies), parts.stats().followers(), parts.stats().following(),
                parts.stats().followedByMe(), parts.recommendations()));
    }

    // An account without a profile is normal (nothing saved yet), so its 404 is not an error here.
    private Uni<Profile> profileOrNull(String userId, String authorization) {
        return profiles.get(userId, authorization).onFailure(Upstream::notFound).recoverWithNull();
    }

    private static String uuid(String id) {
        try {
            return UUID.fromString(id).toString();
        } catch (IllegalArgumentException e) {
            throw new NotFoundException();
        }
    }
}
