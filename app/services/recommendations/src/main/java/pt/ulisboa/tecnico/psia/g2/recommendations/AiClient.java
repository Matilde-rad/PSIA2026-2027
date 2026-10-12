package pt.ulisboa.tecnico.psia.g2.recommendations;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "ai")
public interface AiClient {

    @POST
    @Path("/embed/query")
    Embedding embedQuery(EmbedQuery request);

    record EmbedQuery(String text) {
    }

    record Embedding(float[] embedding) {
    }
}
