package pt.ulisboa.tecnico.psia.g2.ai;

import java.util.List;
import java.util.Map;

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
    @Path("/chat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ChatResponse chat(ChatRequest request);

    record ChatRequest(
            String model,
            List<Message> messages,
            boolean stream,
            Map<String, Object> format,
            Map<String, Object> options,
            @JsonProperty("keep_alive") String keepAlive) {
    }

    record Message(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(
            Message message,
            Boolean done,
            @JsonProperty("done_reason") String doneReason,
            @JsonProperty("total_duration") Long totalDuration,
            @JsonProperty("load_duration") Long loadDuration,
            @JsonProperty("prompt_eval_count") Integer promptEvalCount,
            @JsonProperty("prompt_eval_duration") Long promptEvalDuration,
            @JsonProperty("eval_count") Integer evalCount,
            @JsonProperty("eval_duration") Long evalDuration) {
    }
}
