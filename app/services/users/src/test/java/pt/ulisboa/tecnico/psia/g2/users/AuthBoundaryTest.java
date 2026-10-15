package pt.ulisboa.tecnico.psia.g2.users;

import static org.hamcrest.Matchers.equalTo;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.loginToken;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.post;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.uniqueUsername;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.valid;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.smallrye.jwt.build.Jwt;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AuthBoundaryTest {

    @Test
    void registerLoginMeFlow() {
        String name = uniqueUsername();
        String id = post(valid(name)).then().statusCode(201).extract().path("id");
        String token = loginToken(name);

        RestAssured.given()
                .auth().oauth2(token)
                .get("/users/me")
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("username", equalTo(name));
    }

    @Test
    void meWithoutTokenIs401() {
        assertJson401(RestAssured.get("/users/me"));
    }

    @Test
    void getUserByIdNeedsToken() {
        String name = uniqueUsername();
        String id = post(valid(name)).then().statusCode(201).extract().path("id");

        assertJson401(RestAssured.get("/users/" + id));

        RestAssured.given()
                .auth().oauth2(loginToken(name))
                .get("/users/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id));
    }

    @Test
    void expiredTokenIs401() {
        Instant now = Instant.now();
        String token = Jwt.subject(UUID.randomUUID().toString())
                .issuedAt(now.minusSeconds(4200))
                .expiresAt(now.minusSeconds(600))
                .sign();

        assertJson401(RestAssured.given().auth().oauth2(token).get("/users/me"));
    }

    @Test
    void tokenSignedWithAnotherKeyIs401() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String token = Jwt.issuer("g2-users")
                .subject(UUID.randomUUID().toString())
                .expiresIn(300)
                .sign(generator.generateKeyPair().getPrivate());

        assertJson401(RestAssured.given().auth().oauth2(token).get("/users/me"));
    }

    private static void assertJson401(Response response) {
        response.then()
                .statusCode(401)
                .contentType(ContentType.JSON)
                .body("error", equalTo("unauthorized"));
    }
}
