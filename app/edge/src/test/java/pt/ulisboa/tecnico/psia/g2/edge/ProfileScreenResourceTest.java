package pt.ulisboa.tecnico.psia.g2.edge;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProfileScreenResourceTest {

    @InjectMock
    @RestClient
    UsersClient users;

    @InjectMock
    @RestClient
    RecommendationsClient recommendations;

    @Test
    void ownScreenIncludesRecommendations() {
        String id = UUID.randomUUID().toString();
        String token = Tokens.valid(id);
        String bearer = "Bearer " + token;
        when(users.get(id, bearer))
                .thenReturn(Uni.createFrom().item(new User(id, "alice", "2026-10-08T12:00:00Z")));
        when(recommendations.forUser(id, bearer))
                .thenReturn(Uni.createFrom().item(List.of(new Recommendation("m1", "Arrival", "you like sci-fi"))));

        Map<String, Object> body = RestAssured.given()
                .auth().oauth2(token)
                .get("/screens/profile/" + id.toUpperCase(Locale.ROOT))
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(id))
                .body("username", equalTo("alice"))
                .body("createdAt", equalTo("2026-10-08T12:00:00Z"))
                .body("recommendations.size()", equalTo(1))
                .body("recommendations[0].movieId", equalTo("m1"))
                .extract().as(new io.restassured.common.mapper.TypeRef<Map<String, Object>>() { });

        assertEquals(Set.of("id", "username", "createdAt", "recommendations"), body.keySet());
        verify(users).get(id, bearer);
        verify(recommendations).forUser(id, bearer);
    }

    @Test
    void otherUsersScreenHasNoRecommendations() {
        String caller = UUID.randomUUID().toString();
        String other = UUID.randomUUID().toString();
        String token = Tokens.valid(caller);
        when(users.get(other, "Bearer " + token))
                .thenReturn(Uni.createFrom().item(new User(other, "bob", "2026-10-08T12:00:00Z")));

        RestAssured.given()
                .auth().oauth2(token)
                .get("/screens/profile/" + other)
                .then()
                .statusCode(200)
                .body("username", equalTo("bob"))
                .body("recommendations", empty());

        verify(users).get(other, "Bearer " + token);
        verifyNoInteractions(recommendations);
    }

    @Test
    void malformedIdIs404WithoutUpstreamCalls() {
        RestAssured.given()
                .auth().oauth2(Tokens.valid(UUID.randomUUID().toString()))
                .get("/screens/profile/not-a-uuid")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("error", equalTo("not_found"));

        verifyNoInteractions(users, recommendations);
    }

    @Test
    void failingRecommendationsMakeTheWholeScreen502() {
        String id = UUID.randomUUID().toString();
        String token = Tokens.valid(id);
        String bearer = "Bearer " + token;
        when(users.get(id, bearer))
                .thenReturn(Uni.createFrom().item(new User(id, "alice", "2026-10-08T12:00:00Z")));
        when(recommendations.forUser(id, bearer))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("recommendations is down")));

        RestAssured.given()
                .auth().oauth2(token)
                .get("/screens/profile/" + id)
                .then()
                .statusCode(502)
                .body("error", equalTo("bad_gateway"))
                .body("message", equalTo("upstream service unavailable"))
                .body(not(containsString("recommendations is down")))
                .body(not(containsString("alice")));
    }
}
