package pt.ulisboa.tecnico.psia.g2.edge;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
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
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Test;

@QuarkusTest
class HomeScreenResourceTest {

    @InjectMock
    @RestClient
    RecommendationsClient recommendations;

    @InjectMock
    @RestClient
    ProfilesClient profiles;

    @InjectMock
    @RestClient
    WatchlistClient watchlist;

    @Test
    void homeReturnsTheCallersRecommendations() {
        String id = UUID.randomUUID().toString();
        String token = Tokens.valid(id);
        String bearer = "Bearer " + token;
        when(recommendations.forUser(id, bearer))
                .thenReturn(Uni.createFrom().item(List.of(new Recommendation("m1", "Arrival", null, null))));

        RestAssured.given()
                .auth().oauth2(token)
                .get("/screens/home")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("recommendations.size()", equalTo(1))
                .body("recommendations[0].movieId", equalTo("m1"))
                .body("recommendations[0].title", equalTo("Arrival"));

        verify(recommendations).forUser(id, bearer);
    }

    @Test
    void moreSendsTheCallersOwnTasteAndTheShownIds() {
        String id = UUID.randomUUID().toString();
        String token = Tokens.valid(id);
        String bearer = "Bearer " + token;
        Profile profile = new Profile(id, "Ana", "bio", List.of("Sci-Fi"), List.of(), List.of("1"));
        when(profiles.get(id, bearer)).thenReturn(Uni.createFrom().item(profile));
        when(watchlist.list(bearer)).thenReturn(Uni.createFrom().item(List.of(
                new MovieEntry("5", "liked", null), new MovieEntry("6", "disliked", null),
                new MovieEntry("8", "to_watch", null))));
        Taste taste = new Taste(id, "bio", List.of("Sci-Fi"), List.of(), List.of("1"), List.of("5"), List.of("6"));
        when(recommendations.more(id, new MoreRequest(taste, List.of("7")), bearer))
                .thenReturn(Uni.createFrom().item(List.of(new Recommendation("m2", "Dune", null, null))));

        RestAssured.given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("{\"exclude\":[\"7\"]}")
                .post("/screens/home/more")
                .then()
                .statusCode(200)
                .body("recommendations[0].movieId", equalTo("m2"));

        verify(recommendations).more(id, new MoreRequest(taste, List.of("7")), bearer);
    }

    @Test
    void moreWithoutAProfileIsEmpty() {
        String id = UUID.randomUUID().toString();
        String token = Tokens.valid(id);
        when(profiles.get(id, "Bearer " + token)).thenReturn(
                Uni.createFrom().failure(new ClientWebApplicationException(Response.status(404).build())));

        RestAssured.given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("{\"exclude\":[]}")
                .post("/screens/home/more")
                .then()
                .statusCode(200)
                .body("recommendations.size()", equalTo(0));

        verifyNoInteractions(recommendations);
    }

    @Test
    void failingRecommendationsMakeHome502() {
        String id = UUID.randomUUID().toString();
        String token = Tokens.valid(id);
        when(recommendations.forUser(id, "Bearer " + token))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("recommendations is down")));

        RestAssured.given()
                .auth().oauth2(token)
                .get("/screens/home")
                .then()
                .statusCode(502)
                .body("error", equalTo("bad_gateway"))
                .body("message", equalTo("upstream service unavailable"))
                .body(not(containsString("recommendations is down")));
    }

    @Test
    void noTokenIs401() {
        RestAssured.given()
                .get("/screens/home")
                .then()
                .statusCode(401)
                .contentType(ContentType.JSON)
                .body("error", equalTo("unauthorized"));

        verifyNoInteractions(recommendations);
    }
}
