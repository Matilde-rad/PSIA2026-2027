package pt.ulisboa.tecnico.psia.g2.edge;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MovieScreenResourceTest {

    private static final MoviesClient.MovieDetails ALIEN = new MoviesClient.MovieDetails("1214", "Alien (1979)",
            1979, List.of("Horror", "Sci-Fi"), "poster", "banner", "A crew meets a deadly lifeform.", 117,
            List.of("Sigourney Weaver", "Tom Skerritt"), List.of("Ridley Scott"), 8.4);

    @InjectMock
    @RestClient
    MoviesClient movies;

    @InjectMock
    @RestClient
    WatchlistClient watchlist;

    private static <T> Uni<T> notFound() {
        return Uni.createFrom().failure(new ClientWebApplicationException(
                Response.status(404).entity("{\"error\":\"not_found\"}").build()));
    }

    @Test
    void theScreenHasTheDetailsAndTheCallersStatus() {
        String token = Tokens.valid(UUID.randomUUID().toString());
        when(movies.details("1214")).thenReturn(Uni.createFrom().item(ALIEN));
        when(watchlist.get("1214", "Bearer " + token))
                .thenReturn(Uni.createFrom().item(new MovieEntry("1214", "to_watch", "2026-10-12T10:00:00Z")));

        RestAssured.given().auth().oauth2(token)
                .get("/screens/movie/1214")
                .then()
                .statusCode(200)
                .body("movieId", equalTo("1214"))
                .body("title", equalTo("Alien (1979)"))
                .body("description", equalTo("A crew meets a deadly lifeform."))
                .body("runtimeMinutes", equalTo(117))
                .body("cast", contains("Sigourney Weaver", "Tom Skerritt"))
                .body("directors", contains("Ridley Scott"))
                .body("imdbRating", equalTo(8.4f))
                .body("genres", contains("Horror", "Sci-Fi"))
                .body("status", equalTo("to_watch"));
    }

    @Test
    void aMovieThatIsNotOnTheCallersListHasNoStatus() {
        String token = Tokens.valid(UUID.randomUUID().toString());
        when(movies.details("1214")).thenReturn(Uni.createFrom().item(ALIEN));
        when(watchlist.get(any(), any())).thenReturn(notFound());

        RestAssured.given().auth().oauth2(token)
                .get("/screens/movie/1214")
                .then()
                .statusCode(200)
                .body("title", equalTo("Alien (1979)"))
                .body("status", nullValue());
    }

    @Test
    void anUnknownMovieIs404() {
        String token = Tokens.valid(UUID.randomUUID().toString());
        when(movies.details("999")).thenReturn(notFound());
        when(watchlist.get(any(), any())).thenReturn(notFound());

        RestAssured.given().auth().oauth2(token).get("/screens/movie/999").then().statusCode(404);
    }

    @Test
    void anIdThatIsNotANumberIs404WithoutAskingUpstream() {
        String token = Tokens.valid(UUID.randomUUID().toString());

        RestAssured.given().auth().oauth2(token)
                .get("/screens/movie/alien")
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));

        verifyNoInteractions(movies, watchlist);
    }

    @Test
    void noTokenIs401() {
        RestAssured.get("/screens/movie/1214").then().statusCode(401);

        verifyNoInteractions(movies, watchlist);
    }
}
