package pt.ulisboa.tecnico.psia.g2.edge;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WatchlistScreenResourceTest {

    @InjectMock
    @RestClient
    WatchlistClient watchlist;

    @InjectMock
    @RestClient
    MoviesClient movies;

    @InjectMock
    @RestClient
    ProfilesClient profiles;

    @InjectMock
    @RestClient
    RecommendationsClient recommendations;

    private static MovieEntry entry(String movieId, String status) {
        return new MovieEntry(movieId, status, "2026-10-12T10:00:00Z");
    }

    @Test
    void theScreenSplitsToWatchFromWatchedWithTitlesFromOneLookup() {
        String token = Tokens.valid(UUID.randomUUID().toString());
        when(watchlist.list("Bearer " + token)).thenReturn(Uni.createFrom().item(List.of(
                entry("2571", "liked"), entry("1214", "to_watch"), entry("593", "disliked"),
                entry("404404", "to_watch"))));
        when(movies.lookup(new MoviesClient.LookupRequest(List.of("2571", "1214", "593", "404404"))))
                .thenReturn(Uni.createFrom().item(List.of(
                        new MoviesClient.CatalogMovie("2571", "Matrix, The (1999)", 1999, "matrix.jpg", null),
                        new MoviesClient.CatalogMovie("1214", "Alien (1979)", 1979, "alien.jpg", null),
                        new MoviesClient.CatalogMovie("593", "Silence of the Lambs, The (1991)", 1991, null, null))));

        RestAssured.given().auth().oauth2(token)
                .get("/screens/watchlist")
                .then()
                .statusCode(200)
                .body("toWatch.movieId", contains("1214"))
                .body("toWatch[0].title", equalTo("Alien (1979)"))
                .body("toWatch[0].posterUrl", equalTo("alien.jpg"))
                .body("toWatch[0].status", equalTo("to_watch"))
                .body("watched.movieId", contains("2571", "593"))
                .body("watched.status", contains("liked", "disliked"))
                .body("watched[0].updatedAt", equalTo("2026-10-12T10:00:00Z"));
    }

    @Test
    void anEmptyListNeedsNoMovieLookup() {
        String token = Tokens.valid(UUID.randomUUID().toString());
        when(watchlist.list("Bearer " + token)).thenReturn(Uni.createFrom().item(List.of()));

        RestAssured.given().auth().oauth2(token)
                .get("/screens/watchlist")
                .then()
                .statusCode(200)
                .body("toWatch", empty())
                .body("watched", empty());

        verifyNoInteractions(movies);
    }

    @Test
    void savingAndRemovingForwardTheTokenToUsers() {
        String token = Tokens.valid(UUID.randomUUID().toString());
        String bearer = "Bearer " + token;
        when(watchlist.save("1214", new StatusRequest("liked"), bearer))
                .thenReturn(Uni.createFrom().item(entry("1214", "liked")));
        when(watchlist.remove("1214", bearer)).thenReturn(Uni.createFrom().item(Response.noContent().build()));

        RestAssured.given().auth().oauth2(token).contentType(ContentType.JSON).body(Map.of("status", "liked"))
                .put("/me/watchlist/1214")
                .then()
                .statusCode(200)
                .body("movieId", equalTo("1214"))
                .body("status", equalTo("liked"));
        RestAssured.given().auth().oauth2(token).delete("/me/watchlist/1214").then().statusCode(204);

        verify(watchlist).remove("1214", bearer);
    }

    @Test
    void markingAMovieRefreshesTheCallersRecommendationsInTheBackground() {
        String id = UUID.randomUUID().toString();
        String token = Tokens.valid(id);
        String bearer = "Bearer " + token;
        when(watchlist.save("1214", new StatusRequest("disliked"), bearer))
                .thenReturn(Uni.createFrom().item(entry("1214", "disliked")));
        when(profiles.get(id, bearer)).thenReturn(Uni.createFrom().item(
                new Profile(id, "Ana", "bio", List.of("Horror"), List.of(), List.of())));
        when(watchlist.list(bearer)).thenReturn(Uni.createFrom().item(List.of(entry("1214", "disliked"))));
        Taste taste = new Taste(id, "bio", List.of("Horror"), List.of(), List.of(), List.of(), List.of("1214"));
        when(recommendations.refresh(id, taste, bearer)).thenReturn(Uni.createFrom().item(List.of()));

        RestAssured.given().auth().oauth2(token).contentType(ContentType.JSON).body(Map.of("status", "disliked"))
                .put("/me/watchlist/1214")
                .then()
                .statusCode(200);

        verify(recommendations, timeout(5000)).refresh(id, taste, bearer);
    }

    @Test
    void aStatusUsersRejectsComesBackUnchanged() {
        String token = Tokens.valid(UUID.randomUUID().toString());
        when(watchlist.save("1214", new StatusRequest("loved"), "Bearer " + token)).thenReturn(
                Uni.createFrom().failure(new ClientWebApplicationException(Response.status(400)
                        .entity("{\"error\":\"validation\",\"message\":\"invalid request\"}").build())));

        RestAssured.given().auth().oauth2(token).contentType(ContentType.JSON).body(Map.of("status", "loved"))
                .put("/me/watchlist/1214")
                .then()
                .statusCode(400)
                .body("message", equalTo("invalid request"));
    }

    @Test
    void aMovieIdThatIsNotANumberIs404() {
        String token = Tokens.valid(UUID.randomUUID().toString());

        RestAssured.given().auth().oauth2(token).contentType(ContentType.JSON).body(Map.of("status", "liked"))
                .put("/me/watchlist/alien")
                .then().statusCode(404);
        RestAssured.given().auth().oauth2(token).delete("/me/watchlist/alien").then().statusCode(404);

        verifyNoInteractions(watchlist);
    }

    @Test
    void noTokenIs401() {
        RestAssured.get("/screens/watchlist").then().statusCode(401);
        RestAssured.given().delete("/me/watchlist/1214").then().statusCode(401);

        verifyNoInteractions(watchlist, movies);
    }
}
