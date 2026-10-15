package pt.ulisboa.tecnico.psia.g2.edge;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/me/following")
@Authenticated
public class FollowingResource {

    @Inject
    @RestClient
    SocialClient social;

    @PUT
    @Path("/{userId}")
    public Uni<Response> follow(@PathParam("userId") String userId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        return social.follow(userId, authorization).map(r -> Response.noContent().build());
    }

    @DELETE
    @Path("/{userId}")
    public Uni<Response> unfollow(@PathParam("userId") String userId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        return social.unfollow(userId, authorization).map(r -> Response.noContent().build());
    }
}
