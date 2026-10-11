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
    RecommendationsClient recommendations;

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("/{id}")
    public Uni<ProfileView> get(@PathParam("id") String id,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        UUID target;
        try {
            target = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException();
        }
        String userId = target.toString();
        boolean own = target.equals(UUID.fromString(jwt.getSubject()));

        if (!own) {
            return users.get(userId, authorization).map(user -> ProfileView.of(user, List.of()));
        }
        return Uni.combine().all()
                .unis(users.get(userId, authorization), recommendations.forUser(userId, authorization))
                .with(ProfileView::of);
    }
}
