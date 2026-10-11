package pt.ulisboa.tecnico.psia.g2.edge;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ResponseStatus;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Inject
    @RestClient
    UsersClient users;

    @POST
    @Path("/register")
    @PermitAll
    @ResponseStatus(201)
    public Uni<User> register(RegisterRequest request) {
        if (request == null) {
            throw new BadRequestException();
        }
        return users.register(request);
    }

    @POST
    @Path("/login")
    @PermitAll
    public Uni<LoginResponse> login(LoginRequest request) {
        if (request == null) {
            throw new BadRequestException();
        }
        return users.login(request);
    }
}
