package pt.ulisboa.tecnico.psia.g2.edge;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FeedScreenResourceTest {

    @InjectMock
    @RestClient
    ActivityClient activity;

    @InjectMock
    @RestClient
    MoviesClient movies;

    private static Activity activity(String username, String movieId, String status) {
        return new Activity(UUID.randomUUID().toString(), username, null, movieId, status, "2026-10-12T10:00:00Z");
    }

    private void catalogKnowsAlienAndTheMatrix() {
        when(movies.lookup(any())).thenReturn(Uni.createFrom().item(List.of(
                new MoviesClient.CatalogMovie("1214", "Alien (1979)", 1979, "alien.jpg", null),
                new MoviesClient.CatalogMovie("2571", "Matrix, The (1999)", 1999, null, null))));
    }

    @Test
    void theFeedCarriesMoviesWithTitlesAndPosters() {
        String token = Tokens.valid(UUID.randomUUID().toString());
        when(activity.feed("Bearer " + token)).thenReturn(Uni.createFrom().item(List.of(
                activity("bob", "1214", "liked"), activity("carol", "2571", "to_watch"),
                activity("bob", "404404", "disliked"))));   // not in the catalog: left out
        catalogKnowsAlienAndTheMatrix();

        RestAssured.given().auth().oauth2(token)
                .get("/screens/feed")
                .then()
                .statusCode(200)
                .body("username", contains("bob", "carol"))
                .body("status", contains("liked", "to_watch"))
                .body("movie.movieId", contains("1214", "2571"))
                .body("[0].movie.title", equalTo("Alien (1979)"))
                .body("[0].movie.posterUrl", equalTo("alien.jpg"))
                .body("[0].at", equalTo("2026-10-12T10:00:00Z"));
    }

    @Test
    void anEmptyFeedNeedsNoMovieLookup() {
        String token = Tokens.valid(UUID.randomUUID().toString());
        when(activity.feed("Bearer " + token)).thenReturn(Uni.createFrom().item(List.of()));

        RestAssured.given().auth().oauth2(token).get("/screens/feed").then().statusCode(200).body("$", empty());

        verifyNoInteractions(movies);
    }

    @Test
    void aProfilesActivityIsThatUsers() {
        String token = Tokens.valid(UUID.randomUUID().toString());
        String other = UUID.randomUUID().toString();
        when(activity.of(other, "Bearer " + token))
                .thenReturn(Uni.createFrom().item(List.of(activity("bob", "2571", "disliked"))));
        catalogKnowsAlienAndTheMatrix();

        RestAssured.given().auth().oauth2(token)
                .get("/screens/profile/" + other + "/activity")
                .then()
                .statusCode(200)
                .body("username", contains("bob"))
                .body("status", contains("disliked"))
                .body("[0].movie.title", equalTo("Matrix, The (1999)"));
        RestAssured.given().auth().oauth2(token).get("/screens/profile/not-a-uuid/activity").then().statusCode(404);
    }

    @Test
    void noTokenIs401() {
        RestAssured.get("/screens/feed").then().statusCode(401);

        verifyNoInteractions(activity, movies);
    }
}
