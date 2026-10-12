package pt.ulisboa.tecnico.psia.g2.movies;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

// Loads the precomputed movie vectors (data/embeddings, Git LFS) into movies_db in the background
// after the seed. Skipped once every movie has a vector (a persistent movies_db).
@ApplicationScoped
public class EmbeddingLoader {

    private static final Logger LOG = Logger.getLogger(EmbeddingLoader.class);
    private static final int CHUNK = 1000;

    @Inject
    CatalogRepository catalog;

    @ConfigProperty(name = "movies.embeddings.file")
    String file;

    private volatile boolean stopped;

    // Runs after MovieLensSeeder (default priority), so the movie table is filled.
    void onStart(@Observes @Priority(Interceptor.Priority.APPLICATION + 1000) StartupEvent event) {
        Thread.ofVirtual().name("embedding-loader").start(this::run);
    }

    void onStop(@Observes ShutdownEvent event) {
        stopped = true;
    }

    private void run() {
        try {
            load();
            // Builds the index after a bulk load, or restores it after a crash in the middle of one; a no-op otherwise.
            catalog.createEmbeddingIndex();
            LOG.infof("embeddings ready: %d movies", catalog.embedded());
        } catch (IOException | RuntimeException e) {
            LOG.errorf("embeddings not loaded, the search finds nothing until a restart error=%s", e);
        }
    }

    // movielens_id,"[v1,...,v1024]" per line: qwen3-embedding:0.6b over
    // "Title (year). Genres: ... . Tags: up to 15 most frequent MovieLens tags".
    private void load() throws IOException {
        Path path = Path.of(file);
        if (!Files.exists(path)) {
            LOG.errorf("embeddings: no file at %s", path.toAbsolutePath());
            return;
        }
        if (catalog.missingEmbeddings() == 0) {
            return;
        }
        long start = System.nanoTime();
        int loaded = 0;
        Map<Integer, String> chunk = new LinkedHashMap<>();
        catalog.dropEmbeddingIndex();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            reader.readLine(); // header
            String line;
            while ((line = reader.readLine()) != null && !stopped) {
                int comma = line.indexOf(',');
                if (comma < 1) {
                    continue;
                }
                String literal = line.substring(comma + 1).strip();
                if (literal.startsWith("\"")) {
                    literal = literal.substring(1, literal.length() - 1);
                }
                chunk.put(Integer.parseInt(line.substring(0, comma).strip()), literal);
                if (chunk.size() == CHUNK) {
                    catalog.storeVectorLiterals(chunk);
                    loaded += chunk.size();
                    chunk.clear();
                }
            }
        }
        if (!chunk.isEmpty()) {
            catalog.storeVectorLiterals(chunk);
            loaded += chunk.size();
        }
        if (loaded == 0) {
            LOG.errorf("embeddings: no vectors in %s; without git lfs it is only a pointer, run git lfs pull", path);
        }
        LOG.infof("embeddings: %d loaded from %s in %ds", loaded, path, (System.nanoTime() - start) / 1_000_000_000);
    }
}
