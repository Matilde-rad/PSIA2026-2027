package pt.ulisboa.tecnico.psia.g2.movies;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

// hardcoded response.
@Path("/movies")
public class MovieResource {

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Movie get(@PathParam("id") String id) {
        return new Movie(id, "title-" + id, 2026);
    }
}
