package pt.ulisboa.tecnico.psia.g2.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class OllamaWrapper {

    @Inject
    @RestClient
    OllamaClient client;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "ai.model")
    String model;

    @ConfigProperty(name = "ai.options.num-ctx")
    int numCtx;

    @ConfigProperty(name = "ai.options.keep-alive")
    String keepAlive;

    // TODO Is this fully needed?
    @ConfigProperty(name = "ai.options.num-thread")
    Optional<Integer> numThread;

    public <T> T chat(String task, String system, String user, Map<String, Object> schema,
            int numPredict, Class<T> type) {

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("temperature", 0);
        options.put("seed", 42);
        options.put("num_ctx", numCtx);
        options.put("num_predict", numPredict);
        numThread.ifPresent(n -> options.put("num_thread", n));

        OllamaClient.ChatRequest request = new OllamaClient.ChatRequest(
                model,
                List.of(new OllamaClient.Message("system", system),
                        new OllamaClient.Message("user", user)),
                false,
                schema,
                options,
                keepAlive);

        OllamaClient.ChatResponse response = client.chat(request);

        Log.infof("ollama task=%s done_reason=%s prompt_tokens=%d prompt_ms=%d eval_tokens=%d eval_ms=%d load_ms=%d total_ms=%d",
                task,
                response == null ? null : response.doneReason(),
                count(response == null ? null : response.promptEvalCount()),
                millis(response == null ? null : response.promptEvalDuration()),
                count(response == null ? null : response.evalCount()),
                millis(response == null ? null : response.evalDuration()),
                millis(response == null ? null : response.loadDuration()),
                millis(response == null ? null : response.totalDuration()));

        return parse(objectMapper, response, type);
    }

    static <T> T parse(ObjectMapper mapper, OllamaClient.ChatResponse response, Class<T> type) {
        if (response == null || response.message() == null) {
            throw new ModelOutputException("Ollama returned no message", null);
        }
        if (!"stop".equals(response.doneReason())) {
            throw new ModelOutputException("Ollama done_reason is " + response.doneReason()
                    + ", expected stop", null);
        }
        try {
            return mapper.readValue(response.message().content(), type);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new ModelOutputException("Model content is not valid JSON for " + type.getSimpleName(), e);
        }
    }

    private static int count(Integer value) {
        return value == null ? 0 : value;
    }

    private static long millis(Long nanos) {
        return nanos == null ? 0 : nanos / 1_000_000;
    }

    public static class ModelOutputException extends RuntimeException {
        public ModelOutputException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
