package pt.ulisboa.tecnico.psia.g2.ai;

import java.util.List;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class EmbedTexts {

    public record QueryRequest(String text) {
    }

    public record Embedding(float[] embedding) {
    }

    public static class ModelUnavailableException extends RuntimeException {
        public ModelUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static final String MANDATORY_QUERY_INSTRUCTION =
            "Instruct: Given a description of the movies a user wants to watch, retrieve movies that match it\nQuery: ";

    @Inject
    @RestClient
    OllamaClient client;

    @ConfigProperty(name = "ai.embed.model")
    String model;

    @ConfigProperty(name = "ai.options.keep-alive")
    String keepAlive;

    public Embedding query(QueryRequest request) {
        String text = request == null || request.text() == null ? "" : request.text();
        List<String> input = List.of(MANDATORY_QUERY_INSTRUCTION + text);
        long start = System.nanoTime();
        OllamaClient.EmbedResponse response;
        try {
            response = client.embed(new OllamaClient.EmbedRequest(model, input, keepAlive));
        } catch (RuntimeException e) {
            throw new ModelUnavailableException("Ollama embed call failed", e);
        }
        if (response == null || response.embeddings() == null || response.embeddings().size() != input.size()) {
            throw new ModelUnavailableException("Ollama returned the wrong number of embeddings", null);
        }
        Log.infof("ollama task=embed ms=%d", (System.nanoTime() - start) / 1_000_000);
        return new Embedding(response.embeddings().getFirst());
    }
}
