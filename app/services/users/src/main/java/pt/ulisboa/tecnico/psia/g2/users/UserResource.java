package pt.ulisboa.tecnico.psia.g2.users;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

// hardcoded response.
@Path("/users")
public class UserResource {

    public record UserTaste(String id, String taste) {
    }

    static final List<UserTaste> STUB_USERS = List.of(
            new UserTaste("1", "80s horror"),
            new UserTaste("2", "romantic comedies from the 90s"),
            new UserTaste("3", "space adventure sci-fi from the 2000s"));

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserTaste> list() {
        return STUB_USERS;
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public User get(@PathParam("id") String id) {
        return new User(id, "user-" + id, "User " + id);
    }
}
