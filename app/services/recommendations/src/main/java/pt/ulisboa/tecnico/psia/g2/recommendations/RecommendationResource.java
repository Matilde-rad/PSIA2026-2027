package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

// hardcoded response.
@Path("/recommendations")
public class RecommendationResource {

    @GET
    @Path("/users/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Recommendation> forUser(@PathParam("userId") String userId) {
        return List.of(
                new Recommendation("1", "title-1", "placeholder reason for user " + userId),
                new Recommendation("2", "title-2", "placeholder reason for user " + userId));
    }
}
