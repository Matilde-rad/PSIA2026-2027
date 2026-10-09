package pt.ulisboa.tecnico.psia.g2.ai;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

@Path("/")
public class AiResource {

    @Inject
    ExtractFilters extractFilters;

    @Inject
    RankCandidates rankCandidates;

    @POST
    @Path("/extract-filters")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ExtractFilters.Filters extractFilters(ExtractFilters.Request request) {
        return extractFilters.extract(request);
    }

    @POST
    @Path("/rank")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RankCandidates.Response rank(RankCandidates.Request request) {
        return rankCandidates.rank(request);
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, String>> modelUnavailable(OllamaWrapper.ModelUnavailableException e) {
        return RestResponse.status(Response.Status.SERVICE_UNAVAILABLE, Map.of("error", "model_unavailable"));
    }
}
