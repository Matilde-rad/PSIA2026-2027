package pt.ulisboa.tecnico.psia.g2.edge;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SearchScreenResourceTest {

    @InjectMock
    @RestClient
    RecommendationsClient recommendations;

    @Test
    void searchReturnsTheCallersResults() {
        String id = UUID.randomUUID().toString();
        String token = Tokens.valid(id);
        String bearer = "Bearer " + token;
        SearchRequest request = new SearchRequest("80s horror");
        when(recommendations.search(id, request, bearer)).thenReturn(Uni.createFrom().item(new SearchView(
                List.of(new Recommendation("1258", "Shining, The (1980)", "poster", "banner")),
                new SearchView.AppliedFilters(List.of("Horror"), 1980, 1989))));

        RestAssured.given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("{\"text\":\"80s horror\"}")
                .post("/screens/search")
                .then()
                .statusCode(200)
                .body("results[0].movieId", equalTo("1258"))
                .body("results[0].posterUrl", equalTo("poster"))
                .body("appliedFilters.genres[0]", equalTo("Horror"))
                .body("appliedFilters.yearFrom", equalTo(1980));

        verify(recommendations).search(id, request, bearer);
    }

    @Test
    void aiUnavailableIs503() {
        String id = UUID.randomUUID().toString();
        String token = Tokens.valid(id);
        when(recommendations.search(id, new SearchRequest("x"), "Bearer " + token)).thenReturn(
                Uni.createFrom().failure(new WebApplicationException(Response.status(503).build())));

        RestAssured.given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("{\"text\":\"x\"}")
                .post("/screens/search")
                .then()
                .statusCode(503)
                .body("error", equalTo("ai_unavailable"));
    }

    @Test
    void otherFailuresAre502() {
        String id = UUID.randomUUID().toString();
        String token = Tokens.valid(id);
        when(recommendations.search(id, new SearchRequest("x"), "Bearer " + token))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("recommendations is down")));

        RestAssured.given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("{\"text\":\"x\"}")
                .post("/screens/search")
                .then()
                .statusCode(502)
                .body("error", equalTo("bad_gateway"));
    }

    @Test
    void noTokenIs401() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"text\":\"x\"}")
                .post("/screens/search")
                .then()
                .statusCode(401);

        verifyNoInteractions(recommendations);
    }
}
