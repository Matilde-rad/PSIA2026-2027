package pt.ulisboa.tecnico.psia.g2.recommendations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

@Path("/recommendations")
public class RecommendationResource {

    @Inject
    RecommendationCache cache;

    @Inject
    SemanticSearch semantic;

    @Inject
    RecommendationAgent agent;

    // A new draw on every visit, so Home does not look frozen between agent runs.
    @GET
    @Path("/users/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Recommendation> forUser(@PathParam("userId") String userId) {
        List<Recommendation> picks = new ArrayList<>(cache.read(userId));
        Collections.shuffle(picks);
        return picks;
    }

    @POST
    @Path("/users/{userId}/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SearchResponse search(@PathParam("userId") String userId, SearchRequest request) {
        return request == null ? semantic.search(null, List.of()) : semantic.search(request.text(), request.exclude());
    }

    @POST
    @Path("/users/{userId}/more")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Recommendation> more(@PathParam("userId") String userId, MoreRequest request) {
        return request == null ? List.of() : agent.more(request.profile(), request.exclude());
    }

    @POST
    @Path("/users/{userId}/refresh")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Recommendation> refresh(@PathParam("userId") String userId, TasteProfile profile) {
        return agent.refresh(userId, profile);
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, String>> aiUnavailable(SemanticSearch.AiUnavailableException e) {
        return RestResponse.status(Response.Status.SERVICE_UNAVAILABLE,
                Map.of("error", "ai_unavailable", "message", "AI service down"));
    }
}
