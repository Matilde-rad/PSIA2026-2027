package pt.ulisboa.tecnico.psia.g2.ai;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.ProcessingException;
import org.junit.jupiter.api.Test;

class EmbedTextsTest {

    private final List<OllamaClient.EmbedRequest> sent = new ArrayList<>();

    private EmbedTexts embedTexts(OllamaClient client) {
        EmbedTexts embedTexts = new EmbedTexts();
        embedTexts.client = client;
        embedTexts.model = "qwen3-embedding:0.6b";
        embedTexts.keepAlive = "30m";
        return embedTexts;
    }

    private OllamaClient replying(List<float[]> embeddings) {
        return request -> {
            sent.add(request);
            return new OllamaClient.EmbedResponse(embeddings);
        };
    }

    @Test
    void queryIsSentWithTheInstructionAndItsVectorReturned() {
        float[] vector = {0.6f, 0.8f};
        EmbedTexts.Embedding embedding = embedTexts(replying(List.of(vector)))
                .query(new EmbedTexts.QueryRequest("movies in the water"));

        assertArrayEquals(vector, embedding.embedding());
        assertEquals(List.of(EmbedTexts.MANDATORY_QUERY_INSTRUCTION + "movies in the water"), sent.getFirst().input());
        assertEquals("qwen3-embedding:0.6b", sent.getFirst().model());
    }

    @Test
    void clientFailureIsModelUnavailable() {
        EmbedTexts embedTexts = embedTexts(request -> {
            throw new ProcessingException(new ConnectException("refused"));
        });

        assertThrows(EmbedTexts.ModelUnavailableException.class,
                () -> embedTexts.query(new EmbedTexts.QueryRequest("x")));
    }

    @Test
    void noVectorIsModelUnavailable() {
        EmbedTexts embedTexts = embedTexts(replying(List.of()));

        assertThrows(EmbedTexts.ModelUnavailableException.class,
                () -> embedTexts.query(new EmbedTexts.QueryRequest("x")));
    }
}
