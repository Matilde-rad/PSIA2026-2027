package pt.ulisboa.tecnico.psia.g2.edge;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/me")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class MeResource {

    @Inject
    @RestClient
    UsersClient users;

    @GET
    public Uni<User> me(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        return users.me(authorization);
    }
}
