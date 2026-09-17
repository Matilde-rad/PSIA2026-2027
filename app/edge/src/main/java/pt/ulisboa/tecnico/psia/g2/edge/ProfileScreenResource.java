package pt.ulisboa.tecnico.psia.g2.edge;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/screens/profile")
public class ProfileScreenResource {

    @Inject
    @RestClient
    UsersClient users;

    @Inject
    @RestClient
    RecommendationsClient recommendations;

    @GET
    @Path("/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<ProfileView> get(@PathParam("userId") String userId) {
        // make the two calls in parallel just testing
        return Uni.combine().all()
                .unis(users.get(userId), recommendations.forUser(userId))
                .with((user, recs) -> new ProfileView(userId, user.displayName(), recs));
    }
}
