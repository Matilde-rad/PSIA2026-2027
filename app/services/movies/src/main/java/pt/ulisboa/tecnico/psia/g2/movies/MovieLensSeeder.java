package pt.ulisboa.tecnico.psia.g2.movies;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MovieLensSeeder {

    private static final Logger LOG = Logger.getLogger(MovieLensSeeder.class);
    private static final String ZIP_NAME = "ml-32m.zip";
    private static final String MOVIES_ENTRY = "ml-32m/movies.csv";
    private static final String RATINGS_ENTRY = "ml-32m/ratings.csv";
    private static final String LINKS_ENTRY = "ml-32m/links.csv";
    private static final int BATCH_SIZE = 1000;

    @Inject
    AgroalDataSource dataSource;

    @ConfigProperty(name = "movies.seed.url")
    String seedUrl;

    @ConfigProperty(name = "movies.seed.cache-dir")
    String cacheDir;

    void onStart(@Observes StartupEvent event) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            int existing = count(connection);
            if (existing > 0) {
                LOG.infof("MovieLens already seeded (%d movies), skipping", existing);
                return;
            }
            Path zip = ensureZip();
            Map<Integer, Integer> ratingCounts = new HashMap<>();
            Map<Integer, String> imdbIds = new HashMap<>();
            List<MovieLensCsv.MovieRow> rows = new ArrayList<>();
            readZip(zip, ratingCounts, imdbIds, rows);
            insert(connection, rows, ratingCounts, imdbIds);
            long withoutYear = rows.stream().filter(r -> r.year() == null).count();
            long withoutGenres = rows.stream().filter(r -> r.genres().isEmpty()).count();
            LOG.infof("Seeded %d movies (%d without year, %d without genres)", rows.size(), withoutYear, withoutGenres);
        }
    }

    private static int count(Connection connection) throws SQLException {
        try (var st = connection.createStatement(); ResultSet rs = st.executeQuery("SELECT count(*) FROM movie")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private Path ensureZip() throws IOException, InterruptedException {
        Path dir = Path.of(cacheDir);
        Path zip = dir.resolve(ZIP_NAME);
        if (Files.exists(zip)) {
            LOG.infof("Using cached MovieLens zip at %s", zip);
            return zip;
        }
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, "ml-32m", ".part");
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(seedUrl)).timeout(Duration.ofMinutes(10)).GET().build();
            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tmp));
            if (response.statusCode() != 200) {
                throw new IOException("MovieLens download failed with HTTP " + response.statusCode() + " from " + seedUrl);
            }
            Files.move(tmp, zip, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(tmp);
        }
        LOG.infof("Downloaded MovieLens to %s", zip);
        return zip;
    }

    private static void readZip(Path zip, Map<Integer, Integer> ratingCounts, Map<Integer, String> imdbIds,
            List<MovieLensCsv.MovieRow> rows) throws IOException {
        boolean sawMovies = false;
        boolean sawRatings = false;
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // The reader is deliberately not closed: that would close the zip stream before the next entry.
                BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                if (RATINGS_ENTRY.equals(entry.getName())) {
                    sawRatings = true;
                    reader.readLine();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty()) {
                            continue;
                        }
                        // ratings.csv has no quote characters, so a plain split is safe.
                        String[] f = line.split(",");
                        ratingCounts.merge(Integer.parseInt(f[1]), 1, Integer::sum);
                    }
                } else if (LINKS_ENTRY.equals(entry.getName())) {
                    reader.readLine();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty()) {
                            continue;
                        }
                        // no quote characters here either
                        String[] f = line.split(",");
                        imdbIds.put(Integer.parseInt(f[0]), "tt" + f[1]);
                    }
                } else if (MOVIES_ENTRY.equals(entry.getName())) {
                    sawMovies = true;
                    reader.readLine();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.isEmpty()) {
                            rows.add(MovieLensCsv.parseMovieLine(line));
                        }
                    }
                }
            }
        }
        if (!sawMovies || !sawRatings) {
            throw new IOException("MovieLens zip is missing " + MOVIES_ENTRY + " or " + RATINGS_ENTRY);
        }
    }

    private static void insert(Connection connection, List<MovieLensCsv.MovieRow> rows, Map<Integer, Integer> ratingCounts,
            Map<Integer, String> imdbIds) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO movie (id, title, year, genres, rating_count, imdb_id) VALUES (?, ?, ?, ?, ?, ?)")) {
            int pending = 0;
            for (MovieLensCsv.MovieRow row : rows) {
                ps.setInt(1, row.id());
                ps.setString(2, row.title());
                if (row.year() == null) {
                    ps.setNull(3, Types.INTEGER);
                } else {
                    ps.setInt(3, row.year());
                }
                ps.setArray(4, connection.createArrayOf("text", row.genres().toArray()));
                ps.setInt(5, ratingCounts.getOrDefault(row.id(), 0));
                ps.setString(6, imdbIds.get(row.id()));
                ps.addBatch();
                if (++pending % BATCH_SIZE == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }
}
