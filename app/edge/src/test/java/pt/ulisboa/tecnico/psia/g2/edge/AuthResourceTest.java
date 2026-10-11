package pt.ulisboa.tecnico.psia.g2.edge;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import java.util.UUID;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AuthResourceTest {

    private static final String BODY = "{\"username\":\"alice\",\"password\":\"password1\"}";

    @InjectMock
    @RestClient
    UsersClient users;

    @Test
    void registerReturns201WithTheUserAndNoLocation() {
        String id = UUID.randomUUID().toString();
        when(users.register(new RegisterRequest("alice", "password1")))
                .thenReturn(Uni.createFrom().item(new User(id, "alice", "2026-10-08T12:00:00Z")));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(BODY)
                .post("/auth/register")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .header("Location", nullValue())
                .body("id", equalTo(id))
                .body("username", equalTo("alice"))
                .body("createdAt", equalTo("2026-10-08T12:00:00Z"));

        verify(users).register(new RegisterRequest("alice", "password1"));
    }

    @Test
    void loginReturnsTheTokenResponse() {
        String id = UUID.randomUUID().toString();
        when(users.login(new LoginRequest("alice", "password1")))
                .thenReturn(Uni.createFrom().item(new LoginResponse("header.payload.signature",
                        "2026-10-08T13:00:00Z", new User(id, "alice", "2026-10-08T12:00:00Z"))));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(BODY)
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("token", equalTo("header.payload.signature"))
                .body("expiresAt", equalTo("2026-10-08T13:00:00Z"))
                .body("user.id", equalTo(id))
                .body("user.username", equalTo("alice"));
    }
}
