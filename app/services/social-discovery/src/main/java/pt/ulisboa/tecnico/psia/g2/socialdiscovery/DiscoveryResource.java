package pt.ulisboa.tecnico.psia.g2.socialdiscovery;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;
import java.util.UUID;

@Path("/discover/users")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @RestClient
    UsersClient users;

    @GET
    public List<Profile> list(
            @QueryParam("genre") String genre,
            @QueryParam("director") String director) {

        List<Profile> all = users.listAll();

        if (genre != null && !genre.isBlank()) {
            String g = genre.toLowerCase();
            all = all.stream()
                    .filter(p -> p.favoriteGenres != null &&
                                 p.favoriteGenres.stream().anyMatch(f -> f.toLowerCase().contains(g)))
                    .toList();
        }

        if (director != null && !director.isBlank()) {
            String d = director.toLowerCase();
            all = all.stream()
                    .filter(p -> p.favoriteDirectors != null &&
                                 p.favoriteDirectors.stream().anyMatch(f -> f.toLowerCase().contains(d)))
                    .toList();
        }

        return all;
    }

    @GET
    @Path("/{userId}")
    public Profile get(@PathParam("userId") UUID userId) {
        return users.getById(userId);
    }
}
