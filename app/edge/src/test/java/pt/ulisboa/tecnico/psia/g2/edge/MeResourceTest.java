package pt.ulisboa.tecnico.psia.g2.edge;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.smallrye.mutiny.Uni;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
class MeResourceTest {

    @InjectMock
    @RestClient
    UsersClient users;

    @Test
    void meReturnsTheCallersAccountAndForwardsTheirToken() {
        String id = UUID.randomUUID().toString();
        String token = Tokens.valid(id);
        when(users.me("Bearer " + token))
                .thenReturn(Uni.createFrom().item(new User(id, "alice", "2026-10-08T12:00:00Z")));

        RestAssured.given()
                .auth().oauth2(token)
                .get("/me")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(id))
                .body("username", equalTo("alice"))
                .body("createdAt", equalTo("2026-10-08T12:00:00Z"));

        verify(users).me("Bearer " + token);
    }

    @ParameterizedTest
    @ValueSource(strings = {"no token", "expired", "wrong key"})
    void badTokenIs401BeforeAnyUpstreamCall(String kind) throws Exception {
        String sub = UUID.randomUUID().toString();
        RequestSpecification request = RestAssured.given();
        switch (kind) {
            case "expired" -> request.auth().oauth2(Tokens.expired(sub));
            case "wrong key" -> request.auth().oauth2(Tokens.wrongKey(sub));
            default -> { }
        }

        request.get("/me")
                .then()
                .statusCode(401)
                .contentType(ContentType.JSON)
                .body("error", equalTo("unauthorized"));

        verifyNoInteractions(users);
    }

    @Test
    void usersFailureIs502() {
        when(users.me(anyString()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("users is down")));

        RestAssured.given()
                .auth().oauth2(Tokens.valid(UUID.randomUUID().toString()))
                .get("/me")
                .then()
                .statusCode(502)
                .body("error", equalTo("bad_gateway"))
                .body("message", equalTo("upstream service unavailable"))
                .body(not(containsString("users is down")));
    }
}
