package pt.ulisboa.tecnico.psia.g2.users;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

// hardcoded response.
@Path("/users")
public class UserResource {

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public User get(@PathParam("id") String id) {
        return new User(id, "user-" + id, "User " + id);
    }
}
