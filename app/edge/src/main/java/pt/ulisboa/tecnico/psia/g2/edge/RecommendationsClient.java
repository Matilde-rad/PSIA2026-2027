package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

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

@Path("/recommendations")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "recommendations")
public interface RecommendationsClient {

    @GET
    @Path("/users/{userId}")
    Uni<List<Recommendation>> forUser(@PathParam("userId") String userId,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);

    @POST
    @Path("/users/{userId}/search")
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<SearchView> search(@PathParam("userId") String userId, SearchRequest request,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);

    @POST
    @Path("/users/{userId}/more")
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<List<Recommendation>> more(@PathParam("userId") String userId, MoreRequest request,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);

    @POST
    @Path("/users/{userId}/refresh")
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<List<Recommendation>> refresh(@PathParam("userId") String userId, Taste taste,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);
}
