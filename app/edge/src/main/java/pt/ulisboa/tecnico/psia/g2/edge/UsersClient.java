package pt.ulisboa.tecnico.psia.g2.edge;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "users")
public interface UsersClient {

    @POST
    @Path("/users")
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<User> register(RegisterRequest request);

    @POST
    @Path("/auth/login")
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<LoginResponse> login(LoginRequest request);

    @GET
    @Path("/users/me")
    Uni<User> me(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);

    @GET
    @Path("/users/{id}")
    Uni<User> get(@PathParam("id") String id, @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);
}
