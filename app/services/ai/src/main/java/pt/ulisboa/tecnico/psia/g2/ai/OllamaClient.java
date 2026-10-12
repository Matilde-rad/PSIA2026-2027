package pt.ulisboa.tecnico.psia.g2.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api")
@RegisterRestClient(configKey = "ollama")
public interface OllamaClient {

    @POST
    @Path("/embed")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    EmbedResponse embed(EmbedRequest request);

    record EmbedRequest(String model, List<String> input, @JsonProperty("keep_alive") String keepAlive) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmbedResponse(List<float[]> embeddings) {
    }
}
