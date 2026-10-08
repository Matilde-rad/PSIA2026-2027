package pt.ulisboa.tecnico.psia.g2.users;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Locale;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    UserRepository users;

    @Inject
    PasswordHasher passwordHasher;

    @Inject
    TokenIssuer tokens;

    @POST
    @Path("/login")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    public LoginResponse login(@Valid @NotNull LoginRequest request) {
        String username = request.username().toLowerCase(Locale.ROOT);
        User user = users.findByUsername(username).orElse(null);

        if (!passwordHasher.matches(request.password(), user != null ? user.getPasswordHash() : null)) {
            throw new WebApplicationException("invalid username or password", Response.Status.UNAUTHORIZED);
        }

        TokenIssuer.IssuedToken issued = tokens.issue(user);
        return new LoginResponse(issued.token(), issued.expiresAt(), UserView.from(user));
    }
}
