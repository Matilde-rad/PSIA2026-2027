package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/recommendations")
public class RecommendationResource {

    @Inject
    SearchPipeline  pipeline;

    @GET
    @Path("/users/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Recommendation> forUser(@PathParam("userId") String userId) {
        return List.of(
                new Recommendation("1", "title-1"),
                new Recommendation("2", "title-2"));
    }

    @POST
    @Path("/users/{userId}/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SearchResponse search(@PathParam("userId") String userId, SearchRequest request) {
        return pipeline.search(request == null ? null : request.text());
    }
}
