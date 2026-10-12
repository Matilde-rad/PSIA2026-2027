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
    EmbedTexts embedTexts;

    @POST
    @Path("/embed/query")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public EmbedTexts.Embedding embedQuery(EmbedTexts.QueryRequest request) {
        return embedTexts.query(request);
    }

    @ServerExceptionMapper
    public RestResponse<Map<String, String>> modelUnavailable(EmbedTexts.ModelUnavailableException e) {
        return RestResponse.status(Response.Status.SERVICE_UNAVAILABLE, Map.of("error", "model_unavailable"));
    }
}
