package pt.ulisboa.tecnico.psia.g2.movies;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class Cinemeta {

    private static final Logger LOG = Logger.getLogger(Cinemeta.class);
    private static final Pattern MINUTES = Pattern.compile("(\\d+)\\s*min");
    private static final Pattern HOURS = Pattern.compile("(\\d+)\\s*h");

    // Cinemeta answers an unknown id with a redirect to its live backend, which then says 404.
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Inject
    ObjectMapper mapper;

    @ConfigProperty(name = "movies.meta.url")
    String metaUrl;

    @ConfigProperty(name = "movies.meta.timeout")
    Duration timeout;

    public Optional<MovieMeta> fetch(String imdbId) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(metaUrl.formatted(imdbId)))
                    .timeout(timeout).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.debugf("Cinemeta has no meta for %s (HTTP %d)", imdbId, response.statusCode());
                return Optional.empty();
            }
            return parse(mapper.readTree(response.body()));
        } catch (IOException | RuntimeException e) {
            LOG.warnf("Cinemeta lookup failed imdbId=%s error=%s", imdbId, e.getClass().getSimpleName());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    static Optional<MovieMeta> parse(JsonNode body) {
        JsonNode meta = body == null ? null : body.get("meta");
        if (meta == null || !meta.isObject() || meta.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new MovieMeta(text(meta.get("description")), minutes(text(meta.get("runtime"))),
                names(meta.get("cast")), names(meta.get("director")), rating(text(meta.get("imdbRating")))));
    }

    // "117 min", and now and then "1h 57min"
    static Integer minutes(String runtime) {
        if (runtime == null) {
            return null;
        }
        Matcher hours = HOURS.matcher(runtime);
        Matcher minutes = MINUTES.matcher(runtime);
        boolean hasHours = hours.find();
        boolean hasMinutes = minutes.find();
        if (!hasHours && !hasMinutes) {
            return null;
        }
        int total = (hasHours ? 60 * Integer.parseInt(hours.group(1)) : 0)
                + (hasMinutes ? Integer.parseInt(minutes.group(1)) : 0);
        return total > 0 ? total : null;
    }

    private static Double rating(String text) {
        try {
            return text == null ? null : Double.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<String> names(JsonNode list) {
        List<String> names = new ArrayList<>();
        if (list != null && list.isArray()) {
            list.forEach(n -> {
                String name = text(n);
                if (name != null) {
                    names.add(name);
                }
            });
        }
        return names;
    }

    // Cinemeta HTML-escapes some of its strings: "Dan O&apos;Bannon".
    private static String text(JsonNode node) {
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            return null;
        }
        return node.asText().strip()
                .replace("&apos;", "'").replace("&#39;", "'").replace("&quot;", "\"")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
    }
}
