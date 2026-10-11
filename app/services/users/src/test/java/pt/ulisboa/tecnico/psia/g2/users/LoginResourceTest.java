package pt.ulisboa.tecnico.psia.g2.users;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.login;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.loginToken;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.post;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.uniqueUsername;
import static pt.ulisboa.tecnico.psia.g2.users.Registrations.valid;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.jwt.auth.principal.JWTParser;
import jakarta.inject.Inject;
import java.util.Locale;
import java.util.Map;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

@QuarkusTest
class LoginResourceTest {

    @Inject
    JWTParser parser;

    @Test
    void loginReturnsTokenAndUser() {
        String name = uniqueUsername();
        String id = post(valid(name)).then().statusCode(201).extract().path("id");

        login(name, "password1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("token", notNullValue())
                .body("expiresAt", notNullValue())
                .body("user.id", equalTo(id))
                .body("user.username", equalTo(name))
                .body("user", not(hasKey("passwordHash")));
    }

    @Test
    void tokenCarriesUserIdIssuerAndOneHourLifespan() throws Exception {
        String name = uniqueUsername();
        String id = post(valid(name)).then().statusCode(201).extract().path("id");

        JsonWebToken jwt = parser.parse(loginToken(name));

        assertEquals(id, jwt.getSubject());
        assertEquals(name, jwt.getClaim("upn"));
        assertEquals("g2-users", jwt.getIssuer());
        assertEquals(3600L, jwt.getExpirationTime() - jwt.getIssuedAtTime());
    }

    @Test
    void usernameIsCaseInsensitive() {
        String name = uniqueUsername();
        post(valid(name)).then().statusCode(201);

        login(name.toUpperCase(Locale.ROOT), "password1")
                .then()
                .statusCode(200)
                .body("user.username", equalTo(name));
    }

    @Test
    void wrongPasswordAndUnknownUserGetTheSame401() {
        String name = uniqueUsername();
        post(valid(name)).then().statusCode(201);

        String wrongPassword = login(name, "password2")
                .then().statusCode(401).body("error", equalTo("unauthorized")).extract().asString();
        String unknownUser = login(uniqueUsername(), "password1")
                .then().statusCode(401).body("error", equalTo("unauthorized")).extract().asString();

        assertEquals(wrongPassword, unknownUser);
    }

    @Test
    void missingFieldsReturn400() {
        login(Map.of("username", "abc"))
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"))
                .body("fields[0].field", equalTo("password"));
    }
}
